/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.specular;

import kintsugi3d.builder.fit.decomposition.MaterialBasis;
import kintsugi3d.builder.fit.decomposition.SimpleMaterialBasis;
import kintsugi3d.builder.fit.decomposition.SpecularBasisWeights;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.Texture3D;
import kintsugi3d.gl.vecmath.DoubleVector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpecularFitSerializer
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitSerializer.class);
    private static final Pattern CSV_PATTERN = Pattern.compile("\\s*,+\\s*");
    private static final Pattern SIZE_PATTERN = Pattern.compile("-Y (\\d+) \\+X (\\d+)");

    private SpecularFitSerializer()
    {
    }

    public static void saveWeightImages(int basisCount, int width, int height, SpecularBasisWeights basisWeights, File outputDirectory)
    {
        for (int b = 0; b < basisCount; b++)
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];

            for (int p = 0; p < (width * height); p++)
            {
                float weight = (float) basisWeights.getWeight(b, p);

                // Flip vertically
                int dataBufferIndex = (p % width) + (width * (height - (p / width) - 1));
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(outputDirectory, TextureResources.getUnpackedWeightMapFilename(b, "PNG")));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving weight images:", e);
            }
        }
    }

    public static void saveWeightImages(Texture3D<?> basisWeights, File outputDirectory)
    {
        for (int b = 0; b < basisWeights.getDepth(); b++)
        {
            try
            {
                basisWeights.getColorTextureReader(b).saveToFile("PNG", new File(outputDirectory, TextureResources.getUnpackedWeightMapFilename(b, "PNG")));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving weight images:", e);
            }
        }
    }

    public static void serializeBasisFunctions(int basisCount, int microfacetDistributionResolution, MaterialBasis basis, File outputDirectory, String filenameOverride)
    {
        // Calculate rgbe bytes
        byte[] rgbe = new byte[basisCount * microfacetDistributionResolution * 4];
        for (int b = 0; b < basisCount; ++b)
        {
            for (int m = 0; m < microfacetDistributionResolution; ++m)
            {
                System.arraycopy(doubleToRgbe(basis.evaluateSpecularRed(b, m), basis.evaluateSpecularGreen(b, m), basis.evaluateSpecularBlue(b, m)), 0, rgbe, ((b * m) + m) * 4, 4);
            }
        }

        // Write the bytes out to the HDRI
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, Objects.requireNonNullElseGet(filenameOverride, TextureResources::getBasisFunctionsFilename)))))
        {
            // Write HDR header
            out.write("#?RADIANCE\n".getBytes(StandardCharsets.US_ASCII));
            out.write("FORMAT=32-bit_rle_rgbe\n\n".getBytes(StandardCharsets.US_ASCII));
            out.write(String.format("-Y %d +X %d%n", basisCount, microfacetDistributionResolution).getBytes(StandardCharsets.US_ASCII));

            // Write RGBE data
            out.write(rgbe);
        }
        catch (IOException e)
        {

            LOG.error("An error occurred saving basis functions:", e);
        }
    }

    private static byte[] doubleToRgbe(double r, double g, double b)
    {
        // Find the max color channel for compression
        double maxVal = Math.max(r, Math.max(g, b));
        if (maxVal < 1.0e-32)
        {
            return new byte[4];
        }

        byte[] rgbe = new byte[4];

        // Find the scalar needed to compress values to 8-bit
        FracExp fracExp = frexp(maxVal);
        double scale = (fracExp.fraction * 0x100) / maxVal;

        // Scale all values to RGB 8-bit color channels
        rgbe[0] = (byte) (r * scale);
        rgbe[1] = (byte) (g * scale);
        rgbe[2] = (byte) (b * scale);
        rgbe[3] = (byte) (fracExp.exponent + 0x80); // Standard Radiance dynamic bias offset

        return rgbe;
    }

    // Fraction and exponent
    private static FracExp frexp(double value)
    {
        if (value == 0)
        {
            return new FracExp();
        }

        // The base-2 exponent factor
        int exp = (int) Math.floor(Math.log(value) / Math.log(2)) + 1;
        return new FracExp(value / Math.pow(2, exp), exp);
    }

    /**
     * Deserializes basis functions only.
     * Does not deserialize weights (which can be loaded as images) or diffuse basis colors (which should be re-fit, or a diffuse texture can be used instead).
     *
     * @param priorSolutionDirectory
     * @return An object containing the red, green, and blue basis functions.
     */
    public static MaterialBasis deserializeBasisFunctions(File priorSolutionDirectory) throws IOException
    {
        File basisFile = new File(priorSolutionDirectory, TextureResources.getBasisFunctionsFilename());

        if (!basisFile.exists())
        {
            return null;
        }

        int microfacetDistributionResolution = 0;
        int basisCount = 0;
        byte[] rgbe;

        // Load data file data into RAM first
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(basisFile)))
        {
            StringBuilder builder = new StringBuilder();
            int buf;
            boolean done = false;

            // Parse header until size is found
            while (!done && ((buf = in.read()) != -1))
            {
                // Reset string builder after each line
                if (buf == '\n')
                {
                    String line = builder.toString().trim();
                    builder.setLength(0);
                    Matcher m = SIZE_PATTERN.matcher(line);
                    if (m.matches())
                    {
                        microfacetDistributionResolution = Integer.parseInt(m.group(2));
                        basisCount = Integer.parseInt(m.group(1));
                        // Line after size should always be byte data
                        done = true;
                    }
                }
                else
                {
                    builder.append((char) buf);
                }
            }

            // Parse RGBE byte data
            int index = 0;
            rgbe = new byte[microfacetDistributionResolution * basisCount * 4];
            while (((buf = in.read()) != -1) && (index < (microfacetDistributionResolution * basisCount * 4)))
            {
                rgbe[index] = (byte) buf;
                index++;
            }
        }

        // For final product
        List<double[]> specularRedBasis = new ArrayList<>();
        List<double[]> specularGreenBasis = new ArrayList<>();
        List<double[]> specularBlueBasis = new ArrayList<>();

        // For allocation
        double[] red = new double[microfacetDistributionResolution];
        double[] green = new double[microfacetDistributionResolution];
        double[] blue = new double[microfacetDistributionResolution];
        byte[] data = new byte[4];

        for (int b = 0; b < basisCount; ++b)
        {
            for (int m = 0; m < microfacetDistributionResolution; ++m)
            {
                System.arraycopy(rgbe, ((b * m) + m) * 4, data, 0, 4);
                double[] rgb = rgbeToDouble(data);
                red[m] = rgb[0];
                green[m] = rgb[1];
                blue[m] = rgb[2];
            }
            specularRedBasis.add(red);
            specularGreenBasis.add(green);
            specularBlueBasis.add(blue);
        }

        return new SimpleMaterialBasis(new DoubleVector3[0], specularRedBasis, specularGreenBasis, specularBlueBasis);
    }

    private static double[] rgbeToDouble(byte[] rgbe)
    {
        double[] rgb = new double[3];
        // bitwise operation is for "unsigning" bytes
        if ((rgbe[3] & 0xFF) > 0)
        { // If exponent is 0, pixel is pure black
            // 2^(exponent - Radiance bias - 8) to get a range from 0 to 1
            double factor = Math.pow(2, (rgbe[3] & 0xFF) - 0x80 - 8);
            rgb[0] = (rgbe[0] & 0xFF) * factor;
            rgb[1] = (rgbe[1] & 0xFF) * factor;
            rgb[2] = (rgbe[2] & 0xFF) * factor;
        }
        return rgb;
    }

    private static class FracExp
    {
        final double fraction;
        final int exponent;

        FracExp()
        {
            this.fraction = 0;
            this.exponent = 0;
        }

        FracExp(double fraction, int exponent)
        {
            this.fraction = fraction;
            this.exponent = exponent;
        }
    }
}
