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

package tetzlaff.ibrelight.export.specularfit;

import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.util.Counter;
import tetzlaff.util.NonNegativeLeastSquares;

import static java.lang.Math.PI;

public class WeightOptimization
{
    // For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
    // so we don't want to multiply by n dot l when attempting to reproduce that version.
    private static final boolean OPTIMIZE_REFLECTANCE = SpecularFitRequest.ORIGINAL_NAM_METHOD;

    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;

    private final SpecularFitSettings settings;
    private final double metallicity;

    private final SimpleMatrix[] weightsQTQAugmented;
    private final SimpleMatrix[] weightsQTrAugmented;

    public WeightOptimization(SpecularFitSettings settings, double metallicity)
    {
        this.settings = settings;
        this.metallicity = metallicity;

        weightsQTQAugmented = IntStream.range(0, settings.width * settings.height)
            .mapToObj(p ->
            {
                SimpleMatrix mQTQAugmented = new SimpleMatrix(settings.basisCount + 1, settings.basisCount + 1, DMatrixRMaj.class);

                // Set up equality constraint.
                for (int b = 0; b < settings.basisCount; b++)
                {
                    mQTQAugmented.set(b, settings.basisCount, 1.0);
                    mQTQAugmented.set(settings.basisCount, b, 1.0);
                }

                return mQTQAugmented;
            })
            .toArray(SimpleMatrix[]::new);

        weightsQTrAugmented = IntStream.range(0, settings.width * settings.height)
            .mapToObj(p ->
            {
                SimpleMatrix mQTrAugmented = new SimpleMatrix(settings.basisCount + 1, 1, DMatrixRMaj.class);
                mQTrAugmented.set(settings.basisCount, 1.0); // Set up equality constraint
                return mQTrAugmented;
            })
            .toArray(SimpleMatrix[]::new);
    }

    public void reconstructWeights(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        System.out.println("Building weight fitting matrices...");

        // Setup all the matrices for fitting weights (one per texel)
        buildWeightMatrices(viewStream, solution);

        System.out.println("Finished building matrices; solving now...");

        for (int p = 0; p < settings.width * settings.height; p++)
        {
            if (solution.areWeightsValid(p))
            {
                double median = IntStream.range(0, weightsQTrAugmented[p].getNumElements()).mapToDouble(weightsQTrAugmented[p]::get)
                    .sorted().skip(weightsQTrAugmented[p].getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
                solution.setWeights(p, NonNegativeLeastSquares.solvePremultipliedWithEqualityConstraints(
                    weightsQTQAugmented[p], weightsQTrAugmented[p], median * NNLS_TOLERANCE_SCALE, 1));
            }
        }

        System.out.println("DONE!");

        if (SpecularFitRequest.DEBUG)
        {
            // write out weight textures for debugging
            solution.saveWeightMaps();

            // write out diffuse texture for debugging
            solution.saveDiffuseMap(settings.additional.getFloat("gamma"));
        }
    }

    private void buildWeightMatrices(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        // Initially assume that all texels are invalid.
        solution.invalidateWeights();

        Counter counter = new Counter();

        viewStream.forEach(reflectanceData ->
        {
            // Update matrix for each pixel.
            IntStream.range(0, settings.width * settings.height).parallel().forEach(p ->
            {
                // Skip samples that aren't visible or are otherwise invalid.
                if (reflectanceData.getVisibility(p) > 0)
                {
                    // Any time we have a visible, valid sample, mark that the corresponding texel is valid.
                    solution.setWeightsValidity(p, true);

                    float halfwayIndex = reflectanceData.getHalfwayIndex(p);
                    float geomRatio = reflectanceData.getGeomRatio(p);
                    float addlWeight = reflectanceData.getAdditionalWeight(p);
                    float nDotL = reflectanceData.getNDotL(p);
                    DoubleVector3 fActual = reflectanceData.getColor(p).asDoublePrecision();

                    // Precalculate frequently used values.
                    double mExact = halfwayIndex * settings.microfacetDistributionResolution;
                    double weightSquared = addlWeight * addlWeight;

                    // For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
                    // so we don't want to multiply by n dot l when attempting to reproduce that version.
                    double nDotLSquared = OPTIMIZE_REFLECTANCE ? 1.0 : nDotL * nDotL;

                    int m1 = (int)Math.floor(mExact);
                    int m2 = m1 + 1;
                    double t = mExact - m1;

                    for (int b1 = 0; b1 < settings.basisCount; b1++)
                    {
                        // Evaluate the first basis BRDF.
                        DoubleVector3 f1 = solution.getDiffuseAlbedo(b1).dividedBy(PI);

                        if (m1 < settings.microfacetDistributionResolution)
                        {
                            f1 = f1.plus(new DoubleVector3(solution.getSpecularRed().get(m1, b1), solution.getSpecularGreen().get(m1, b1), solution.getSpecularBlue().get(m1, b1))
                                .times(1.0 - t)
                                .plus(new DoubleVector3(solution.getSpecularRed().get(m2, b1), solution.getSpecularGreen().get(m2, b1), solution.getSpecularBlue().get(m2, b1))
                                    .times(t))
                                .times((double) geomRatio));
                        }
                        else if (metallicity > 0.0f)
                        {
                            f1 = f1.plus(new DoubleVector3(
                                solution.getSpecularRed().get(settings.microfacetDistributionResolution, b1),
                                solution.getSpecularGreen().get(settings.microfacetDistributionResolution, b1),
                                solution.getSpecularBlue().get(settings.microfacetDistributionResolution, b1))
                                .times((double) geomRatio));
                        }

                        // Store the weighted product of the basis BRDF and the actual BRDF in the vector.
                        weightsQTrAugmented[p].set(b1, weightsQTrAugmented[p].get(b1) + weightSquared * nDotLSquared * f1.dot(fActual));

                        for (int b2 = 0; b2 < settings.basisCount; b2++)
                        {
                            // Evaluate the second basis BRDF.
                            DoubleVector3 f2 = solution.getDiffuseAlbedo(b2).dividedBy(PI);

                            if (m1 < settings.microfacetDistributionResolution)
                            {
                                f2 = f2.plus(new DoubleVector3(solution.getSpecularRed().get(m1, b2), solution.getSpecularGreen().get(m1, b2), solution.getSpecularBlue().get(m1, b2))
                                    .times(1.0 - t)
                                    .plus(new DoubleVector3(solution.getSpecularRed().get(m2, b2), solution.getSpecularGreen().get(m2, b2), solution.getSpecularBlue().get(m2, b2))
                                        .times(t))
                                    .times((double) geomRatio));
                            }
                            else if (metallicity > 0.0f)
                            {
                                f2 = f2.plus(new DoubleVector3(
                                    solution.getSpecularRed().get(settings.microfacetDistributionResolution, b2),
                                    solution.getSpecularGreen().get(settings.microfacetDistributionResolution, b2),
                                    solution.getSpecularBlue().get(settings.microfacetDistributionResolution, b2))
                                    .times((double) geomRatio));
                            }

                            // Store the weighted product of the two BRDFs in the matrix.
                            weightsQTQAugmented[p].set(b1, b2, weightsQTQAugmented[p].get(b1, b2) + weightSquared * nDotLSquared * f1.dot(f2));
                        }
                    }
                }
            });

            synchronized (counter)
            {
                System.out.println("Finished view " + counter.get() + '.');
                counter.increment();
            }
        });
    }
}
