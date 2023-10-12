/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit.decomposition;

import java.util.function.IntFunction;

import kintsugi3d.builder.fit.ReflectanceData;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.optimization.LeastSquaresModel;

import static java.lang.Math.PI;

public class SpecularWeightModel implements LeastSquaresModel<ReflectanceData, DoubleVector3>
{
    private final SpecularDecomposition solution;
    private final SpecularBasisSettings specularBasisSettings;

    /**
     *
     * @param solution
     * @param settings
     */
    public SpecularWeightModel(SpecularDecomposition solution, SpecularBasisSettings settings)
    {
        this.solution = solution;
        specularBasisSettings = settings;
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
        return sampleData.getAdditionalWeight(systemIndex);
    }

    @Override
    public DoubleVector3 getSamples(ReflectanceData sampleData, int systemIndex)
    {
        // Sampler (ground truth data)
        return new DoubleVector3(sampleData.getRed(systemIndex), sampleData.getGreen(systemIndex), sampleData.getBlue(systemIndex));
    }

    @Override
    public IntFunction<DoubleVector3> getBasisFunctions(ReflectanceData sampleData, int systemIndex)
    {
        // Precompute values that will be reused; captured by the lambda expression.
        float halfwayIndex = sampleData.getHalfwayIndex(systemIndex);
        float geomRatio = sampleData.getGeomRatio(systemIndex);

        // Precalculate frequently used values.
        double mExact = halfwayIndex * specularBasisSettings.getBasisResolution();

        int m1 = (int)Math.floor(mExact);
        int m2 = m1 + 1;
        double t = mExact - m1;

        return b ->
        {
            // Evaluate the basis BRDF.
            // This will run a lot of times so write out vector math operations
            // to avoid unnecessary allocation of Vector objects
            if (m1 < specularBasisSettings.getBasisResolution())
            {
                return new DoubleVector3(
                    solution.getDiffuseAlbedo(b).x / PI +
                        (solution.getSpecularBasis().evaluateRed(b, m1) * (1 - t)
                            + solution.getSpecularBasis().evaluateRed(b, m2) * t) * geomRatio,
                    solution.getDiffuseAlbedo(b).y / PI +
                        (solution.getSpecularBasis().evaluateGreen(b, m1) * (1 - t)
                            + solution.getSpecularBasis().evaluateGreen(b, m2) * t) * geomRatio,
                    solution.getDiffuseAlbedo(b).z / PI +
                        (solution.getSpecularBasis().evaluateBlue(b, m1) * (1 - t)
                            + solution.getSpecularBasis().evaluateBlue(b, m2) * t) * geomRatio);
            }
            else if (specularBasisSettings.getMetallicity() > 0.0f)
            {
                return new DoubleVector3(
                    solution.getDiffuseAlbedo(b).x / PI +
                        solution.getSpecularBasis().evaluateRed(b, specularBasisSettings.getBasisResolution()) * geomRatio,
                    solution.getDiffuseAlbedo(b).y / PI +
                        solution.getSpecularBasis().evaluateGreen(b, specularBasisSettings.getBasisResolution()) * geomRatio,
                    solution.getDiffuseAlbedo(b).z / PI +
                        solution.getSpecularBasis().evaluateBlue(b, specularBasisSettings.getBasisResolution()) * geomRatio);
            }
            else // if metallicity == 0, then the MDF should be 0 here
            {
                return solution.getDiffuseAlbedo(b).dividedBy(PI);
            }
        };
    }

    @Override
    public int getBasisFunctionCount()
    {
        return specularBasisSettings.getBasisCount();
    }

    @Override
    public double innerProduct(DoubleVector3 t1, DoubleVector3 t2)
    {
        return t1.dot(t2);
    }
}
