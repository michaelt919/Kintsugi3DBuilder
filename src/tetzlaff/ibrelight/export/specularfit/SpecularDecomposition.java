/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.core.TextureFitSettings;

public class SpecularDecomposition implements SpecularBasis, SpecularBasisWeights
{
    private final DoubleVector3[] diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;
    private final SimpleMatrix[] weightsByTexel;
    private final boolean[] weightsValidity;

    private final TextureFitSettings textureFitSettings;
    private final SpecularBasisSettings specularBasisSettings;

    public SpecularDecomposition(TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings)
    {
        this.textureFitSettings = textureFitSettings;
        this.specularBasisSettings = specularBasisSettings;

        diffuseAlbedos = new DoubleVector3[this.specularBasisSettings.getBasisCount()];

        for (int i = 0; i < this.specularBasisSettings.getBasisCount(); i++)
        {
            diffuseAlbedos[i] = DoubleVector3.ZERO;
        }

        specularRed = new SimpleMatrix(
            this.specularBasisSettings.getMicrofacetDistributionResolution() + 1,
            this.specularBasisSettings.getBasisCount(), DMatrixRMaj.class);
        specularGreen = new SimpleMatrix(
            this.specularBasisSettings.getMicrofacetDistributionResolution() + 1,
            this.specularBasisSettings.getBasisCount(), DMatrixRMaj.class);
        specularBlue = new SimpleMatrix(
            this.specularBasisSettings.getMicrofacetDistributionResolution() + 1,
            this.specularBasisSettings.getBasisCount(), DMatrixRMaj.class);

        weightsByTexel = IntStream.range(0, this.textureFitSettings.width * this.textureFitSettings.height)
            .mapToObj(p -> new SimpleMatrix(this.specularBasisSettings.getBasisCount(), 1, DMatrixRMaj.class))
            .toArray(SimpleMatrix[]::new);
        weightsValidity = new boolean[this.textureFitSettings.width * this.textureFitSettings.height];
    }

    @Override
    public double evaluateRed(int b, int m)
    {
        return specularRed.get(m, b);
    }

    @Override
    public double evaluateGreen(int b, int m)
    {
        return specularGreen.get(m, b);
    }

    @Override
    public double evaluateBlue(int b, int m)
    {
        return specularBlue.get(m, b);
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

    public List<SimpleMatrix> getWeightsList()
    {
        return Arrays.asList(weightsByTexel);
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

    public void saveBasisFunctions(File outputDirectory)
    {
        SpecularFitSerializer.serializeBasisFunctions(specularBasisSettings.getBasisCount(),
            specularBasisSettings.getMicrofacetDistributionResolution(), this, outputDirectory);
    }

    public void saveWeightMaps(File outputDirectory)
    {
        SpecularFitSerializer.saveWeightImages(
            specularBasisSettings.getBasisCount(), textureFitSettings.width, textureFitSettings.height, this, outputDirectory);
    }

    public void saveDiffuseMap(double gamma, File outputDirectory)
    {
        BufferedImage diffuseImg = new BufferedImage(textureFitSettings.width, textureFitSettings.height, BufferedImage.TYPE_INT_ARGB);
        int[] diffuseDataPacked = new int[textureFitSettings.width * textureFitSettings.height];
        for (int p = 0; p < textureFitSettings.width * textureFitSettings.height; p++)
        {
            DoubleVector4 diffuseSum = DoubleVector4.ZERO;

            for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
            {
                diffuseSum = diffuseSum.plus(diffuseAlbedos[b].asVector4(1.0)
                    .times(weightsByTexel[p].get(b)));
            }

            if (diffuseSum.w > 0)
            {
                DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / gamma)));

                // Flip vertically
                int dataBufferIndex = p % textureFitSettings.width + textureFitSettings.width * (textureFitSettings.height - p / textureFitSettings.width - 1);
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
            e.printStackTrace();
        }
    }

    public TextureFitSettings getTextureFitSettings()
    {
        return textureFitSettings;
    }

    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }
}
