/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;

public class KMeansClustering
{
    private static final double TOLERANCE = 0.0001;

    private final List<Vector4> colorMap;

    public KMeansClustering(List<Vector4> colorMap)
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

        do
        {
            firstCenterIndex = random.nextInt(colorMap.size());
        }
        while(colorMap.get(firstCenterIndex).w < 1.0); // Make sure the center chosen is valid.

        Vector3[] centers = new Vector3[solutionOut.size()];
        centers[0] = colorMap.get(firstCenterIndex).getXYZ();

        // Populate a CDF for the purpose of randomly selecting from a weighted probability distribution.
        double[] cdf = new double[colorMap.size() + 1];

        for (int b = 1; b < solutionOut.size(); b++)
        {
            cdf[0] = 0.0;

            for (int p = 0; p < colorMap.size(); p++)
            {
                if (colorMap.get(p).w > 0.0)
                {
                    double minDistance = Double.MAX_VALUE;

                    for (int b2 = 0; b2 < b; b2++)
                    {
                        minDistance = Math.min(minDistance, centers[b2].distance(colorMap.get(p).getXYZ()));
                    }

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
            while (index < 0 || colorMap.get(index).w == 0.0)
            {
                // Search forward until a valid index is found.
                index++;

                // We shouldn't ever fail to find an index since x should have been less than the final (un-normalized) CDF total.
                // This means that there has to be some place where the CDF went up, corresponding to a valid index.
                assert index < cdf.length - 1;
            }

            // We've found a new center.
            centers[b] = colorMap.get(index).getXYZ();
        }

        System.out.println("Initial centers:");
        for (int b = 0; b < solutionOut.size(); b++)
        {
            System.out.println(centers[b]);
        }

        // Initialization is done; now it's time to iterate.
        boolean changed;
        do
        {
            // Initialize sums to zero.
            Vector4[] sums = IntStream.range(0, solutionOut.size()).mapToObj(i -> Vector4.ZERO).toArray(Vector4[]::new);

            for (Vector4 color : colorMap)
            {
                if (color.w > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < solutionOut.size(); b++)
                    {
                        double distance = centers[b].distance(color.getXYZ());
                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            bMin = b;
                        }
                    }

                    sums[bMin] = sums[bMin].plus(color.getXYZ().asVector4(1.0f));
                }
            }

            changed = false;
            for (int b = 0; b < solutionOut.size(); b++)
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

                for (int b = 0; b < solutionOut.size(); b++)
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
