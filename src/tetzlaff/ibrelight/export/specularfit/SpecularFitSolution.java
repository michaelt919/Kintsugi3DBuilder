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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;

public class SpecularFitSolution
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

    public List<SimpleMatrix> getWeightsList()
    {
        return Arrays.asList(weightsByTexel);
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

    public void saveBasisFunctions()
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(settings.outputDirectory, "basisFunctions.csv")))
        {
            for (int b = 0; b < settings.basisCount; b++)
            {
                out.print("Red#" + b);
                for (int m = 0; m <= settings.microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(specularRed.get(m, b));
                }
                out.println();

                out.print("Green#" + b);
                for (int m = 0; m <= settings.microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(specularGreen.get(m, b));
                }
                out.println();

                out.print("Blue#" + b);
                for (int m = 0; m <= settings.microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(specularBlue.get(m, b));
                }
                out.println();
            }

            out.println();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public void saveWeightMaps()
    {
        for (int b = 0; b < settings.basisCount; b++)
        {
            BufferedImage weightImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[settings.width * settings.height];

            for (int p = 0; p < settings.width * settings.height; p++)
            {
                float weight = (float)weightsByTexel[p].get(b);

                // Flip vertically
                int dataBufferIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(settings.outputDirectory, String.format("weights%02d.png", b)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
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
}
