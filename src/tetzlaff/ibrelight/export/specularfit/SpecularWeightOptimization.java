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

import java.util.Collections;

import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.optimization.NonNegativeWeightOptimization;

import static java.lang.Math.PI;

public class SpecularWeightOptimization
{
    // For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
    // so we don't want to multiply by n dot l when attempting to reproduce that version.
    private static final boolean OPTIMIZE_REFLECTANCE = SpecularOptimization.ORIGINAL_NAM_METHOD;

    private final SpecularFitSettings settings;
    private final double metallicity;

    private final NonNegativeWeightOptimization base;

    public SpecularWeightOptimization(SpecularFitSettings settings, double metallicity)
    {
        this.settings = settings;
        this.metallicity = metallicity;

        base = new NonNegativeWeightOptimization(settings.width * settings.height, settings.basisCount,
            Collections.singletonList(b -> 1.0), Collections.singletonList(1.0)); // Equality constraint to ensure that the weights sum up to 1.0.
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        System.out.println("Building weight fitting matrices...");

        // Initially assume that all texels are invalid.
        solution.invalidateWeights();

        // Setup all the matrices for fitting weights (one per texel)
        base.buildMatrices(
            // Stream of data coming from the GPU
            viewStream,

            // Visibility test
            (reflectanceData, p) -> reflectanceData.getVisibility(p) > 0,

            // If a pixel is valid in some view, mark it as such in the solution.
            p -> solution.setWeightsValidity(p, true),

            // Weight function.
            // For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
            // so we don't want to multiply by n dot l when attempting to reproduce that version.
            (reflectanceData, p) -> reflectanceData.getAdditionalWeight(p) * (OPTIMIZE_REFLECTANCE ? 1 : reflectanceData.getNDotL(p)),

            // Sampler (ground truth data)
            (reflectanceData, p) -> reflectanceData.getColor(p).asDoublePrecision(),

            // Basis function calculator
            (reflectanceData, p) ->
            {
                // Precompute values that will be reused; captured by the lambda expression.
                float halfwayIndex = reflectanceData.getHalfwayIndex(p);
                float geomRatio = reflectanceData.getGeomRatio(p);

                // Precalculate frequently used values.
                double mExact = halfwayIndex * settings.microfacetDistributionResolution;

                int m1 = (int)Math.floor(mExact);
                int m2 = m1 + 1;
                double t = mExact - m1;

                return b ->
                {
                    // Evaluate the basis BRDF.
                    DoubleVector3 fDiffuse = solution.getDiffuseAlbedo(b).dividedBy(PI);

                    if (m1 < settings.microfacetDistributionResolution)
                    {
                        return fDiffuse
                            .plus(new DoubleVector3(
                                    solution.getSpecularRed().get(m1, b),
                                    solution.getSpecularGreen().get(m1, b),
                                    solution.getSpecularBlue().get(m1, b))
                                .times(1.0 - t)
                            .plus(new DoubleVector3(
                                    solution.getSpecularRed().get(m2, b),
                                    solution.getSpecularGreen().get(m2, b),
                                    solution.getSpecularBlue().get(m2, b))
                                .times(t))
                            .times((double) geomRatio));
                    }
                    else if (metallicity > 0.0f)
                    {
                        return fDiffuse
                            .plus(new DoubleVector3(
                                    solution.getSpecularRed().get(settings.microfacetDistributionResolution, b),
                                    solution.getSpecularGreen().get(settings.microfacetDistributionResolution, b),
                                    solution.getSpecularBlue().get(settings.microfacetDistributionResolution, b))
                                .times((double) geomRatio));
                    }

                    return fDiffuse;
                };
            },
            DoubleVector3::dot);

        System.out.println("Finished building matrices; solving now...");

        // Optimize the weights and store the result in the SpecularFitSolution.
        base.optimizeWeights(solution::areWeightsValid, solution::setWeights);

        System.out.println("DONE!");

        if (SpecularOptimization.DEBUG)
        {
            // write out weight textures for debugging
            solution.saveWeightMaps();

            // write out diffuse texture for debugging
            solution.saveDiffuseMap(settings.additional.getFloat("gamma"));
        }
    }
}