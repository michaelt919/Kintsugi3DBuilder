/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.ibrelight.core.TextureFitSettings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public abstract class SpecularDecompositionBase implements SpecularDecomposition
{
    private final SimpleMatrix[] weightsByTexel;
    private final boolean[] weightsValidity;
    private final TextureFitSettings textureFitSettings;

    protected SpecularDecompositionBase(TextureFitSettings textureFitSettings, int basisCount)
    {
        weightsByTexel = IntStream.range(0, textureFitSettings.width * textureFitSettings.height)
            .mapToObj(p -> new SimpleMatrix(basisCount, 1, DMatrixRMaj.class))
            .toArray(SimpleMatrix[]::new);
        weightsValidity = new boolean[textureFitSettings.width * textureFitSettings.height];
        this.textureFitSettings = textureFitSettings;

    }

    @Override
    public boolean areWeightsValid(int texelIndex)
    {
        return weightsValidity[texelIndex];
    }

    @Override
    public double getWeight(int b, int p)
    {
        return weightsByTexel[p].get(b);
    }

    @Override
    public SimpleMatrix getWeights(int texelIndex)
    {
        return weightsByTexel[texelIndex];
    }

    @Override
    public void setWeights(int texelIndex, SimpleMatrix weights)
    {
        weightsByTexel[texelIndex] = weights;
    }

    @Override
    public List<SimpleMatrix> getWeightsList()
    {
        return Arrays.asList(weightsByTexel);
    }

    @Override
    public void invalidateWeights()
    {
        // Quickly invalidate all the weights
        Arrays.fill(weightsValidity, false);
    }

    @Override
    public void setWeightsValidity(int texelIndex, boolean validity)
    {
        weightsValidity[texelIndex] = validity;
    }

    @Override
    public void saveWeightMaps(File outputDirectory)
    {
        SpecularFitSerializer.saveWeightImages(
            getSpecularBasisSettings().getBasisCount(), textureFitSettings.width, textureFitSettings.height, this, outputDirectory);
    }

    @Override
    public TextureFitSettings getTextureFitSettings()
    {
        return textureFitSettings;
    }
}
