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

import java.util.Arrays;
import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;

public class SpecularFitSolution
{
    private final DoubleVector3[] diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;
    private final SimpleMatrix[] weightsByTexel;
    private final boolean[] weightsValidity;

    private SpecularFitSettings settings;

    public SpecularFitSolution(SpecularFitSettings settings)
    {
        this.settings = settings;

        diffuseAlbedos = new DoubleVector3[settings.basisCount];
        specularRed = new SimpleMatrix(settings.microfacetDistributionResolution + 1, settings.basisCount, DMatrixRMaj.class);
        specularGreen = new SimpleMatrix(settings.microfacetDistributionResolution + 1, settings.basisCount, DMatrixRMaj.class);
        specularBlue = new SimpleMatrix(settings.microfacetDistributionResolution + 1, settings.basisCount, DMatrixRMaj.class);

        weightsByTexel = IntStream.range(0, settings.width * settings.height)
            .mapToObj(p -> new SimpleMatrix(settings.basisCount + 1, 1, DMatrixRMaj.class))
            .toArray(SimpleMatrix[]::new);
        weightsValidity = new boolean[settings.width * settings.height];
    }

    public DoubleVector3 getDiffuseAlbedo(int basisIndex)
    {
        return diffuseAlbedos[basisIndex];
    }

    public void setDiffuseAlbedo(int basisIndex, DoubleVector3 diffuseAlbedo)
    {
        diffuseAlbedos[basisIndex] = diffuseAlbedo;
    }

    public SimpleMatrix getSpecularRed()
    {
        return specularRed;
    }

    public SimpleMatrix getSpecularGreen()
    {
        return specularGreen;
    }

    public SimpleMatrix getSpecularBlue()
    {
        return specularBlue;
    }

    public SimpleMatrix getWeights(int texelIndex)
    {
        return weightsByTexel[texelIndex];
    }

    public void setWeights(int texelIndex, SimpleMatrix weights)
    {
        weightsByTexel[texelIndex] = weights;
    }

    public boolean areWeightsValid(int texelIndex)
    {
        return weightsValidity[texelIndex];
    }

    public void invalidateWeights()
    {
        // Quickly invalidate all the weights
        Arrays.fill(weightsValidity, false);
    }

    public void setWeightsValidity(int texelIndex, boolean validity)
    {
        weightsValidity[texelIndex] = validity;
    }

    public SpecularFitSettings getSettings()
    {
        return settings;
    }
}
