/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.core.TextureFitSettings;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public abstract class SpecularDecompositionBase implements SpecularDecomposition
{
    private static final Logger log = LoggerFactory.getLogger(SpecularDecompositionBase.class);
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
    public TextureFitSettings getTextureFitSettings()
    {
        return textureFitSettings;
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
    public void fillHoles()
    {
        // Fill holes
        // TODO Quick hack; should be replaced with something more robust.
        log.info("Filling holes...");

        int texelCount = textureFitSettings.width * textureFitSettings.height;

        for (int i = 0; i < Math.max(textureFitSettings.width, textureFitSettings.height); i++)
        {
            Collection<Integer> filledPositions = new HashSet<>(256);
            for (int p = 0; p < texelCount; p++)
            {
                if (!this.areWeightsValid(p))
                {
                    int left = (texelCount + p - 1) % texelCount;
                    int right = (p + 1) % texelCount;
                    int up = (texelCount + p - textureFitSettings.width) % texelCount;
                    int down = (p + textureFitSettings.width) % texelCount;

                    int count = 0;

                    for (int b = 0; b < this.getSpecularBasisSettings().getBasisCount(); b++)
                    {
                        count = 0;
                        double sum = 0.0;

                        if (this.areWeightsValid(left))
                        {
                            sum += this.getWeights(left).get(b);
                            count++;
                        }

                        if (this.areWeightsValid(right))
                        {
                            sum += this.getWeights(right).get(b);
                            count++;
                        }

                        if (this.areWeightsValid(up))
                        {
                            sum += this.getWeights(up).get(b);
                            count++;
                        }

                        if (this.areWeightsValid(down))
                        {
                            sum += this.getWeights(down).get(b);
                            count++;
                        }

                        if (sum > 0.0)
                        {
                            this.getWeights(p).set(b, sum / count);
                        }
                    }

                    if (count > 0)
                    {
                        filledPositions.add(p);
                    }
                }
            }

            for (int p : filledPositions)
            {
                this.setWeightsValidity(p, true);
            }
        }

        log.info("DONE!");
    }

    @Override
    public void saveWeightMaps(File outputDirectory)
    {
        SpecularFitSerializer.saveWeightImages(
            getSpecularBasisSettings().getBasisCount(), textureFitSettings.width, textureFitSettings.height, this, outputDirectory);
    }
}