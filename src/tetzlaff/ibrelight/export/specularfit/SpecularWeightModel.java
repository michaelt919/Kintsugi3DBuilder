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

import java.util.function.IntFunction;

import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.optimization.LeastSquaresModel;

import static java.lang.Math.PI;

public class SpecularWeightModel implements LeastSquaresModel<ReflectanceData, DoubleVector3>
{
    private final SpecularFitSolution solution;
    private final SpecularFitSettings settings;
    private final double metallicity;
    private final boolean optimizeReflectance;

    /**
     *
     * @param solution
     * @param settings
     * @param metallicity
     * @param optimizeReflectance Whether or not to optimize reflectance (no cosine weighting) as opposed to radiance (with cosine weighting).
     *                            For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
     *                            so we don't want to multiply by the cosine (n dot l) when attempting to reproduce that version.
     */
    public SpecularWeightModel(SpecularFitSolution solution, SpecularFitSettings settings, double metallicity, boolean optimizeReflectance)
    {
        this.solution = solution;
        this.settings = settings;
        this.metallicity = metallicity;
        this.optimizeReflectance = optimizeReflectance;
    }

    @Override
    public boolean isValid(ReflectanceData sampleData, int systemIndex)
    {
        // Visibility test
        return sampleData.getVisibility(systemIndex) > 0;
    }

    @Override
    public double getSampleWeight(ReflectanceData sampleData, int systemIndex)
    {
        // Don't multiply by n dot l when optimizing reflectance (rather than radiance)
        return sampleData.getAdditionalWeight(systemIndex) * (optimizeReflectance ? 1 : sampleData.getNDotL(systemIndex));
    }

    @Override
    public DoubleVector3 getSamples(ReflectanceData sampleData, int systemIndex)
    {
        // Sampler (ground truth data)
        return sampleData.getColor(systemIndex).asDoublePrecision();
    }

    @Override
    public IntFunction<DoubleVector3> getBasisFunctions(ReflectanceData sampleData, int systemIndex)
    {
        // Precompute values that will be reused; captured by the lambda expression.
        float halfwayIndex = sampleData.getHalfwayIndex(systemIndex);
        float geomRatio = sampleData.getGeomRatio(systemIndex);

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
    }

    @Override
    public double innerProduct(DoubleVector3 t1, DoubleVector3 t2)
    {
        return t1.dot(t2);
    }
}
