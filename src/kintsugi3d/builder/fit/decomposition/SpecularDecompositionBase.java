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

package kintsugi3d.builder.fit.decomposition;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.export.specular.SpecularFitSerializer;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.gl.vecmath.DoubleVector4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

public abstract class SpecularDecompositionBase implements SpecularDecomposition
{
    private static final Logger log = LoggerFactory.getLogger(SpecularDecompositionBase.class);
    private final SimpleMatrix[] weightsByTexel;
    private final boolean[] weightsValidity;
    private final TextureResolution textureResolution;

    protected SpecularDecompositionBase(TextureResolution textureResolution, int basisCount)
    {
        weightsByTexel = IntStream.range(0, textureResolution.width * textureResolution.height)
            .mapToObj(p -> new SimpleMatrix(basisCount, 1, DMatrixRMaj.class))
            .toArray(SimpleMatrix[]::new);
        weightsValidity = new boolean[textureResolution.width * textureResolution.height];
        this.textureResolution = textureResolution;
    }

    @Override
    public SpecularBasisWeights getWeights()
    {
        return new SpecularBasisWeights()
        {
            final int count = getSpecularBasisSettings().getBasisCount();

            @Override
            public double getWeight(int b, int p)
            {
                return weightsByTexel[p].get(b);
            }

            @Override
            public boolean areWeightsValid(int p)
            {
                return weightsValidity[p];
            }

            @Override
            public int getCount()
            {
                return count;
            }

            @Override
            public void save(File outputDirectory)
            {
                SpecularFitSerializer.saveWeightImages(
                    count, textureResolution.width, textureResolution.height, this, outputDirectory);
            }
        };
    }

    @Override
    public TextureResolution getTextureResolution()
    {
        return textureResolution;
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

        int texelCount = textureResolution.width * textureResolution.height;

        for (int i = 0; i < Math.max(textureResolution.width, textureResolution.height); i++)
        {
            Collection<Integer> filledPositions = new HashSet<>(256);
            for (int p = 0; p < texelCount; p++)
            {
                if (!this.areWeightsValid(p))
                {
                    int left = (texelCount + p - 1) % texelCount;
                    int right = (p + 1) % texelCount;
                    int up = (texelCount + p - textureResolution.width) % texelCount;
                    int down = (p + textureResolution.width) % texelCount;

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
    public DoubleVector3 getDiffuseAlbedo(int basisIndex)
    {
        return getDiffuseAlbedos().get(basisIndex);
    }

    @Override
    public void saveDiffuseMap(double gamma, File outputDirectory)
    {
        BufferedImage diffuseImg = new BufferedImage(getTextureResolution().width, getTextureResolution().height, BufferedImage.TYPE_INT_ARGB);
        int[] diffuseDataPacked = new int[getTextureResolution().width * getTextureResolution().height];
        for (int p = 0; p < getTextureResolution().width * getTextureResolution().height; p++)
        {
            DoubleVector4 diffuseSum = DoubleVector4.ZERO;

            for (int b = 0; b < getSpecularBasis().getCount(); b++)
            {
                diffuseSum = diffuseSum.plus(getDiffuseAlbedo(b).asVector4(1.0)
                    .times(getWeight(b, p)));
            }

            if (diffuseSum.w > 0)
            {
                DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / gamma)));

                // Flip vertically
                int dataBufferIndex = p % getTextureResolution().width + getTextureResolution().width * (getTextureResolution().height - p / getTextureResolution().width - 1);
                diffuseDataPacked[dataBufferIndex] = new Color((float) diffuseAvgGamma.x, (float) diffuseAvgGamma.y, (float) diffuseAvgGamma.z).getRGB();
            }
        }

        diffuseImg.setRGB(0, 0, diffuseImg.getWidth(), diffuseImg.getHeight(), diffuseDataPacked, 0, diffuseImg.getWidth());

        try
        {
            ImageIO.write(diffuseImg, "PNG", new File(outputDirectory, "diffuse_frombasis.png"));
        }
        catch (IOException e)
        {
            log.error("An error occurred while saving diffuse map:", e);
        }
    }

    @Override
    public void saveBasisFunctions(File outputDirectory)
    {
        getSpecularBasis().save(outputDirectory);
    }

    @Override
    public void saveWeightMaps(File outputDirectory)
    {
        getWeights().save(outputDirectory);
    }
}
