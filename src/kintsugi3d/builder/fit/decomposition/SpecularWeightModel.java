/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.fit.ReflectanceData;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.optimization.LeastSquaresModel;

import java.util.List;
import java.util.function.IntFunction;

import static java.lang.Math.PI;

public class SpecularWeightModel implements LeastSquaresModel<ReflectanceData, DoubleVector3>
{
    private final SpecularDecomposition solution;

    /**
     *
     * @param solution
     */
    public SpecularWeightModel(SpecularDecomposition solution)
    {
        this.solution = solution;
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
        int specularResolution = solution.getMaterialBasis().getSpecularResolution();
        double mExact = halfwayIndex * specularResolution;

        int m1 = (int)Math.floor(mExact);
        int m2 = m1 + 1;
        double t = mExact - m1;

        // List may contain  both enabled and disabled materials, but the disabled materials shouldn't get accessed.
        List<? extends BasisMaterialInfo> materials = solution.getMaterialBasis().getIndexableMaterialList();

        return b ->
        {
            BasisMaterialInfo material = materials.get(b);

            // Evaluate the basis BRDF.
            // This will run a lot of times so write out vector math operations
            // to avoid unnecessary allocation of Vector objects
            if (m1 < specularResolution)
            {
                return new DoubleVector3(
                    material.getDiffuseColor().x / PI +
                        (material.evaluateSpecularRed(m1) * (1 - t)
                            + material.evaluateSpecularRed(m2) * t) * geomRatio,
                    material.getDiffuseColor().y / PI +
                        (material.evaluateSpecularGreen(m1) * (1 - t)
                            + material.evaluateSpecularGreen(m2) * t) * geomRatio,
                    material.getDiffuseColor().z / PI +
                        (material.evaluateSpecularBlue(m1) * (1 - t)
                            + material.evaluateSpecularBlue(m2) * t) * geomRatio);
            }
            else
            {
                return new DoubleVector3(
                    material.getDiffuseColor().x / PI +
                        material.evaluateSpecularRed(specularResolution) * geomRatio,
                    material.getDiffuseColor().y / PI +
                        material.evaluateSpecularGreen(specularResolution) * geomRatio,
                    material.getDiffuseColor().z / PI +
                        material.evaluateSpecularBlue(specularResolution) * geomRatio);
            }
        };
    }

    @Override
    public int getBasisFunctionCount()
    {
        // Only need enabled materials during optimization.
        return solution.getMaterialBasis().getEnabledMaterialCount();
    }

    @Override
    public double innerProduct(DoubleVector3 t1, DoubleVector3 t2)
    {
        return t1.dot(t2);
    }
}
