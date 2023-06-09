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
import tetzlaff.gl.core.Context;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

public class SpecularFitSolution implements SpecularBasis, SpecularBasisWeights
{
    private final DoubleVector3[] diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;
    private final SimpleMatrix[] weightsByTexel;
    private final boolean[] weightsValidity;

    private final SpecularFitSettings settings;

    public SpecularFitSolution(SpecularFitSettings settings)
    {
        this.settings = settings;

        diffuseAlbedos = new DoubleVector3[settings.basisCount];

        for (int i = 0; i < settings.basisCount; i++)
        {
            diffuseAlbedos[i] = DoubleVector3.ZERO;
        }

        specularRed = new SimpleMatrix(settings.microfacetDistributionResolution + 1, settings.basisCount, DMatrixRMaj.class);
        specularGreen = new SimpleMatrix(settings.microfacetDistributionResolution + 1, settings.basisCount, DMatrixRMaj.class);
        specularBlue = new SimpleMatrix(settings.microfacetDistributionResolution + 1, settings.basisCount, DMatrixRMaj.class);

        weightsByTexel = IntStream.range(0, settings.width * settings.height)
            .mapToObj(p -> new SimpleMatrix(settings.basisCount, 1, DMatrixRMaj.class))
            .toArray(SimpleMatrix[]::new);
        weightsValidity = new boolean[settings.width * settings.height];
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

    public SpecularFitSettings getSettings()
    {
        return settings;
    }

    public void saveBasisFunctions()
    {
        SpecularFitSerializer.serializeBasisFunctions(settings.basisCount, settings.microfacetDistributionResolution, this, settings.outputDirectory);
    }

    public void saveWeightMaps()
    {
        SpecularFitSerializer.saveWeightImages(
            settings.basisCount, settings.width, settings.height, this, settings.outputDirectory);
    }

    public void saveDiffuseMap(double gamma)
    {
        BufferedImage diffuseImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
        int[] diffuseDataPacked = new int[settings.width * settings.height];
        for (int p = 0; p < settings.width * settings.height; p++)
        {
            DoubleVector4 diffuseSum = DoubleVector4.ZERO;

            for (int b = 0; b < settings.basisCount; b++)
            {
                diffuseSum = diffuseSum.plus(diffuseAlbedos[b].asVector4(1.0)
                    .times(weightsByTexel[p].get(b)));
            }

            if (diffuseSum.w > 0)
            {
                DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / gamma)));

                // Flip vertically
                int dataBufferIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);
                diffuseDataPacked[dataBufferIndex] = new Color((float) diffuseAvgGamma.x, (float) diffuseAvgGamma.y, (float) diffuseAvgGamma.z).getRGB();
            }
        }

        diffuseImg.setRGB(0, 0, diffuseImg.getWidth(), diffuseImg.getHeight(), diffuseDataPacked, 0, diffuseImg.getWidth());

        try
        {
            ImageIO.write(diffuseImg, "PNG", new File(settings.outputDirectory, "diffuse_frombasis.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public <ContextType extends Context<ContextType>> void saveGlTF(IBRResources<ContextType> resources)
    {
        System.out.println("Starting glTF export...");
        try
        {
            SpecularFitGltfExporter exporter = SpecularFitGltfExporter.fromVertexGeometry(resources.geometry);
            exporter.setDefaultNames();
            exporter.addWeightImages(settings.basisCount);
            exporter.write(new File(settings.outputDirectory, "model.glb"));
            System.out.println("DONE!");
        } catch (IOException e)
        {
            System.out.println("Error occurred during glTF export:");
            e.printStackTrace();
        }
    }
}
