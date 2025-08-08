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

package kintsugi3d.builder.io.specular;

import kintsugi3d.builder.fit.decomposition.MaterialBasis;
import kintsugi3d.builder.fit.decomposition.SimpleMaterialBasis;
import kintsugi3d.builder.fit.decomposition.SpecularBasisWeights;
import kintsugi3d.gl.core.Texture3D;
import kintsugi3d.gl.vecmath.DoubleVector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public final class SpecularFitSerializer
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitSerializer.class);
    private static final Pattern CSV_PATTERN = Pattern.compile("\\s*,+\\s*");

    private SpecularFitSerializer()
    {
    }

    public static void saveWeightImages(int basisCount, int width, int height, SpecularBasisWeights basisWeights, File outputDirectory)
    {
        for (int b = 0; b < basisCount; b++)
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];

            for (int p = 0; p < width * height; p++)
            {
                float weight = (float)basisWeights.getWeight(b, p);

                // Flip vertically
                int dataBufferIndex = p % width + width * (height - p / width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(outputDirectory, getWeightFileName(b)));
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
                basisWeights.getColorTextureReader(b).saveToFile("PNG", new File(outputDirectory, getWeightFileName(b)));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving weight images:", e);
            }
        }
    }

    public static String getCombinedWeightFilename(int imageIndex)
    {
        return getWeightFileName(imageIndex, 4);
    }

    public static String getWeightFileName(int weightMapIndex)
    {
        return getWeightFileName(weightMapIndex, 1);
    }

    public static String getWeightFileName(int weightMapIndex, int weightsPerChannel)
    {
        int scaledWeightMapIndex = weightMapIndex;
        if (weightsPerChannel <= 1)
        {
            return String.format("weights%02d.png", scaledWeightMapIndex);
        }
        else
        {
            scaledWeightMapIndex *= weightsPerChannel;
            return String.format("weights%02d%02d.png", scaledWeightMapIndex, scaledWeightMapIndex + (weightsPerChannel - 1));
        }
    }

    public static void serializeBasisFunctions(int basisCount, int microfacetDistributionResolution, MaterialBasis basis, File outputDirectory)
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(outputDirectory, getBasisFunctionsFilename()), StandardCharsets.UTF_8))
        {
            for (int b = 0; b < basisCount; b++)
            {
                out.printf("Red#%d", b);
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(basis.evaluateSpecularRed(b, m));
                }
                out.println();

                out.printf("Green#%d", b);
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(basis.evaluateSpecularGreen(b, m));
                }
                out.println();

                out.printf("Blue#%d", b);
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(basis.evaluateSpecularBlue(b, m));
                }
                out.println();
            }

            for (int b = 0; b < basisCount; b++)
            {
                DoubleVector3 diffuseColor = basis.getDiffuseColor(b);
                out.printf("Diffuse#%d, %f, %f, %f", b, diffuseColor.x, diffuseColor.y, diffuseColor.z);
                out.println();
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving basis functions:", e);
        }
    }

    public static String getBasisFunctionsFilename()
    {
        return "basisFunctions.csv";
    }

    /**
     * Deserializes basis functions only.
     * Does not deserialize weights (which can be loaded as images) or diffuse basis colors (which should be re-fit, or a diffuse texture can be used instead).
     * @param priorSolutionDirectory
     * @return An object containing the red, green, and blue basis functions.
     */
    public static MaterialBasis deserializeBasisFunctions(File priorSolutionDirectory)
        throws IOException
    {
        File basisFile = new File(priorSolutionDirectory, getBasisFunctionsFilename());

        if (basisFile.exists())
        {
            // Test to figure out the resolution
            int numElements; // Technically this is "microfacetDistributionResolution + 1" the way it's defined elsewhere
            try (Scanner in = new Scanner(basisFile, StandardCharsets.UTF_8))
            {
                in.useLocale(Locale.US);
                String testLine = in.nextLine();
                String[] elements = CSV_PATTERN.split(testLine);
                if (elements[elements.length - 1].isBlank()) // detect trailing comma
                {
                    // Don't count the blank element after the trailing comma, or the leading identifier on each line.
                    numElements = elements.length - 2;
                }
                else
                {
                    // Don't count the leading identifier on each line.
                    numElements = elements.length - 1;
                }
            }

            // Now actually parse the file
            try (Scanner in = new Scanner(basisFile, StandardCharsets.UTF_8))
            {
                in.useLocale(Locale.US);

                List<double[]> specularRedBasis = new ArrayList<>(8);
                List<double[]> specularGreenBasis = new ArrayList<>(8);
                List<double[]> specularBlueBasis = new ArrayList<>(8);

                in.useDelimiter("\\s*[,\\n\\r]+\\s*"); // CSV

                String currentTag = in.next();
                int b = 0;
                while (!currentTag.startsWith("Diffuse") && in.hasNext()) // stop at end of file or if diffuse albedos found
                {
                    // Beginning a new basis function for each RGB component.
                    specularRedBasis.add(new double[numElements]);
                    specularGreenBasis.add(new double[numElements]);
                    specularBlueBasis.add(new double[numElements]);

                    if (currentTag.equals(String.format("Red#%d", b)))
                    {
                        for (int m = 0; m < numElements; m++)
                        {
                            specularRedBasis.get(b)[m] = in.nextDouble();
                        }
                    }
                    else
                    {
                        throw new IOException(MessageFormat.format("Unexpected line beginning with {0}", currentTag));
                    }
                    // newline

                    currentTag = in.next();
                    if (currentTag.equals(String.format("Green#%d", b)))
                    {
                        for (int m = 0; m < numElements; m++)
                        {
                            specularGreenBasis.get(b)[m] = in.nextDouble();
                        }
                    }
                    else
                    {
                        throw new IOException(MessageFormat.format("Unexpected line beginning with {0}", currentTag));
                    }
                    // newline

                    currentTag = in.next(); // "Blue#{b}"
                    if (currentTag.equals(String.format("Blue#%d", b)))
                    {
                        for (int m = 0; m < numElements; m++)
                        {
                            specularBlueBasis.get(b)[m] = in.nextDouble();
                        }
                    }
                    else
                    {
                        throw new IOException(MessageFormat.format("Unexpected line beginning with {0}", currentTag));
                    }
                    // newline

                    if (in.hasNext())
                    {
                        // Get tag of next element for while loop check
                        currentTag = in.next();
                    }

                    b++;
                }

                DoubleVector3[] diffuseBasis = new DoubleVector3[b]; // "b" is the number of specular basis functions from the earlier loop
                int diffuseCount = 0;

                while (in.hasNext()) // parse diffuse albedos if found
                {
                    if (currentTag.equals(String.format("Diffuse#%d", diffuseCount)))
                    {
                        diffuseBasis[diffuseCount] = new DoubleVector3(in.nextDouble(), in.nextDouble(), in.nextDouble());
                    }
                    else
                    {
                        throw new IOException(MessageFormat.format("Unexpected line beginning with {0}", currentTag));
                    }
                    // newline

                    if (in.hasNext())
                    {
                        // Get tag of next element
                        currentTag = in.next();
                    }

                    diffuseCount++;
                }

                while (diffuseCount < diffuseBasis.length)
                {
                    // Default to black if not found
                    diffuseBasis[diffuseCount] = DoubleVector3.ZERO;
                    diffuseCount++;
                }

                return new SimpleMaterialBasis(
                    diffuseBasis,
                    specularRedBasis.toArray(double[][]::new), specularGreenBasis.toArray(double[][]::new), specularBlueBasis.toArray(double[][]::new));
            }
        }
        else
        {
            return null;
        }
    }
}
