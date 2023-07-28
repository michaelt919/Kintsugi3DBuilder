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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.Texture3D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class SpecularFitSerializer
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitSerializer.class);

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
                log.error("An error occurred saving weight images:", e);
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
                log.error("An error occurred saving weight images:", e);
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
        if (weightsPerChannel <= 1)
        {
            return String.format("weights%02d.png", weightMapIndex);
        }
        else
        {
            weightMapIndex *= weightsPerChannel;
            return String.format("weights%02d%02d.png", weightMapIndex, weightMapIndex + (weightsPerChannel - 1));
        }
    }

    public static void serializeBasisFunctions(int basisCount, int microfacetDistributionResolution, SpecularBasis basis, File outputDirectory)
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(outputDirectory, getBasisFunctionsFilename())))
        {
            for (int b = 0; b < basisCount; b++)
            {
                out.print("Red#" + b);
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(basis.evaluateRed(b, m));
                }
                out.println();

                out.print("Green#" + b);
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(basis.evaluateGreen(b, m));
                }
                out.println();

                out.print("Blue#" + b);
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(basis.evaluateBlue(b, m));
                }
                out.println();
            }

            out.println();
        }
        catch (FileNotFoundException e)
        {
            log.error("An error occurred saving basis functions:", e);
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
    public static SpecularBasis deserializeBasisFunctions(File priorSolutionDirectory)
        throws FileNotFoundException
    {
        File basisFile = new File(priorSolutionDirectory, getBasisFunctionsFilename());

        // Test to figure out the resolution
        int numElements; // Technically this is "microfacetDistributionResolution + 1" the way it's defined elsewhere
        try (Scanner in = new Scanner(basisFile))
        {
            String testLine = in.nextLine();
            String[] elements = testLine.split("\\s*,+\\s*");
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
        try (Scanner in = new Scanner(basisFile))
        {
            List<double[]> redBasis = new ArrayList<>(8);
            List<double[]> greenBasis = new ArrayList<>(8);
            List<double[]> blueBasis = new ArrayList<>(8);

            in.useDelimiter("\\s*[,\\n\\r]+\\s*"); // CSV

            int b = 0;
            while (in.hasNext())
            {
                // Beginning a new basis function for each RGB component.
                redBasis.add(new double[numElements]);
                greenBasis.add(new double[numElements]);
                blueBasis.add(new double[numElements]);

                in.next(); // "Red#{b}"
                for (int m = 0; m < numElements; m++)
                {
                    redBasis.get(b)[m] = in.nextDouble();
                }
                // newline

                in.next(); // "Green#{b}"
                for (int m = 0; m < numElements; m++)
                {
                    greenBasis.get(b)[m] = in.nextDouble();
                }
                // newline

                in.next(); // "Blue#{b}"
                for (int m = 0; m < numElements; m++)
                {
                    blueBasis.get(b)[m] = in.nextDouble();
                }
                // newline

                b++;
            }
            // newline

            return new SimpleSpecularBasis(
                redBasis.toArray(double[][]::new), greenBasis.toArray(double[][]::new), blueBasis.toArray(double[][]::new));
        }
    }
}
