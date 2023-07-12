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

import javax.imageio.ImageIO;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.core.TextureFitSettings;

public class SpecularDecompositionFromScratch extends SpecularDecompositionBase {
    private final DoubleVector3[] diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;

    private final SpecularBasisSettings specularBasisSettings;

    public SpecularDecompositionFromScratch(TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings)
    {
        super(textureFitSettings, specularBasisSettings.getBasisCount());
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

    @Override
    public void saveBasisFunctions(File outputDirectory)
    {
        SpecularFitSerializer.serializeBasisFunctions(specularBasisSettings.getBasisCount(),
            specularBasisSettings.getMicrofacetDistributionResolution(), this, outputDirectory);
    }

    @Override
    public void saveDiffuseMap(double gamma, File outputDirectory)
    {
        BufferedImage diffuseImg = new BufferedImage(getTextureFitSettings().width, getTextureFitSettings().height, BufferedImage.TYPE_INT_ARGB);
        int[] diffuseDataPacked = new int[getTextureFitSettings().width * getTextureFitSettings().height];
        for (int p = 0; p < getTextureFitSettings().width * getTextureFitSettings().height; p++)
        {
            DoubleVector4 diffuseSum = DoubleVector4.ZERO;

            for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
            {
                diffuseSum = diffuseSum.plus(diffuseAlbedos[b].asVector4(1.0)
                    .times(getWeight(b, p)));
            }

            if (diffuseSum.w > 0)
            {
                DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / gamma)));

                // Flip vertically
                int dataBufferIndex = p % getTextureFitSettings().width + getTextureFitSettings().width * (getTextureFitSettings().height - p / getTextureFitSettings().width - 1);
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

    @Override
    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }
}
