/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.optimization;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.ColorArrayList;

public class KMeansClustering
{
    private static final Logger log = LoggerFactory.getLogger(KMeansClustering.class);
    private static final double TOLERANCE = 0.0001;

    private final ColorArrayList colorMap;

    public KMeansClustering(ColorArrayList colorMap)
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.colorMap = colorMap;
    }

    public List<Vector3> makeClusters(List<SimpleMatrix> solutionOut)
    {
        // k-means++ initialization
        Random random = new SecureRandom();

        // Randomly choose the first center.
        int firstCenterIndex;
        int failsafeCounter = 0;

        do
        {
            firstCenterIndex = random.nextInt(colorMap.size());
            failsafeCounter++;
        }
        while(colorMap.getAlpha(firstCenterIndex) < 1.0 && failsafeCounter <= colorMap.size()); // Make sure the center chosen is valid.

        if (colorMap.getAlpha(firstCenterIndex) < 1.0)
        {
            throw new IllegalStateException("Color map does not contain any valid elements.");
        }

        int basisCount = solutionOut.get(0).getNumElements();
        Vector3[] centers = new Vector3[basisCount];
        centers[0] = colorMap.getRGB(firstCenterIndex);

        // Populate a CDF for the purpose of randomly selecting from a weighted probability distribution.
        double[] cdf = new double[colorMap.size() + 1];

        for (int b = 1; b < basisCount; b++)
        {
            cdf[0] = 0.0;

            for (int p = 0; p < colorMap.size(); p++)
            {
                if (colorMap.getAlpha(p) > 0.0)
                {
                    Vector3 sample = colorMap.getRGB(p);

                    // Find the minimum distance from the sample to any of the current centers.
                    double minDistance = IntStream.range(0, b)
                        .mapToDouble(b2 -> centers[b2].distance(sample))
                        .min()
                        .orElse(Double.MAX_VALUE);
                    cdf[p + 1] = cdf[p] + minDistance * minDistance;
                }
            }

            double x = random.nextDouble() * cdf[colorMap.size() - 1];

            //noinspection FloatingPointEquality
            if (x >= cdf[colorMap.size() - 1]) // It's possible but extremely unlikely that floating-point rounding would cause this to happen.
            {
                // In that extremely rare case, just set x to 0.0.
                x = 0.0;
            }

            // binarySearch returns index of a match if its found, or -insertionPoint - 1 if not (the more likely scenario).
            // insertionPoint is defined to be the index of the first element greater than the number searched for (the randomly generated value).
            int index = Arrays.binarySearch(cdf, x);

            if (index < 0) // The vast majority of the time this condition will be true, since floating-point matches are unlikely.
            {
                // We actually want insertionPoint - 1 since the CDF is offset by one from the actual array of colors.
                // i.e. if the random value falls between indices 3 and 4 in the CDF, the differential between those two indices is due to the color
                // at index 3, so we want to use 3 as are index.
                index = -index - 2;
            }

            assert index < cdf.length - 1;

            // If the index was actually positive to begin with, that's probably fine; just make sure that it's a valid location.
            // It's also possible in theory for the index to be zero if the random number generator produced 0.0.
            while (index < 0 || colorMap.getAlpha(index) == 0.0)
            {
                // Search forward until a valid index is found.
                index++;

                // We shouldn't ever fail to find an index since x should have been less than the final (un-normalized) CDF total.
                // This means that there has to be some place where the CDF went up, corresponding to a valid index.
                assert index < cdf.length - 1;
            }

            // We've found a new center.
            centers[b] = colorMap.getRGB(index);
        }

        log.info("Initial centers:");
        for (int b = 0; b < basisCount; b++)
        {
            log.info(centers[b].toString());
        }

        // Initialization is done; now it's time to iterate.
        boolean changed;
        do
        {
            Vector4[] sums = IntStream.range(0, colorMap.size())
                .parallel()
                .collect(
                    // Supplier:
                    () -> IntStream.range(0, basisCount)
                        .mapToObj(i -> Vector4.ZERO) // Initialize sums to zero.
                        .toArray(Vector4[]::new),
                    // Accumulator:
                    (partialSums, p) ->
                    {
                        if (colorMap.getAlpha(p) > 0.0)
                        {
                            int bMin = -1;

                            double minDistance = Double.MAX_VALUE;

                            for (int b = 0; b < basisCount; b++)
                            {
                                double distance = centers[b].distance(colorMap.getRGB(p));
                                if (distance < minDistance)
                                {
                                    minDistance = distance;
                                    bMin = b;
                                }
                            }

                            partialSums[bMin] = partialSums[bMin]
                                .plus(new Vector4(colorMap.getRed(p), colorMap.getGreen(p), colorMap.getBlue(p), 1.0f));
                        }
                    },
                    // Combiner:
                    (sums1, sums2) -> IntStream.range(0, basisCount)
                        .forEach(b -> sums1[b] = sums1[b].plus(sums2[b])));

            changed = false;
            for (int b = 0; b < basisCount; b++)
            {
                if (sums[b].w > 0.0)
                {
                    Vector3 newCenter = sums[b].getXYZ().dividedBy(sums[b].w);
                    changed = changed || newCenter.distance(centers[b]) > TOLERANCE;
                    centers[b] = newCenter;
                }
            }
        }
        while (changed);

        for (int p = 0; p < colorMap.size(); p++)
        {
            // Initialize weights to zero.
            solutionOut.get(p).zero();

            if (colorMap.get(p).w > 0.0)
            {
                int bMin = -1;

                double minDistance = Double.MAX_VALUE;

                for (int b = 0; b < basisCount; b++)
                {
                    double distance = centers[b].distance(colorMap.get(p).getXYZ());
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        bMin = b;
                    }
                }

                // Set weight to one for the cluster that each pixel belongs to.
                solutionOut.get(p).set(bMin, 1.0);
            }
        }

        // weightMapsOut now contain the final clusters.

        // Return the cluster centers.
        return Arrays.asList(centers);
    }
}
