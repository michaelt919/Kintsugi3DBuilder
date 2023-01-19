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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import javax.imageio.ImageIO;

public class SpecularFitSerializer
{
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
                e.printStackTrace();
            }
        }
    }

    public static String getWeightFileName(int weightMapIndex)
    {
        return String.format("weights%02d.png", weightMapIndex);
    }

    public static void serializeBasisFunctions(int basisCount, int microfacetDistributionResolution, SpecularBasis basis, File outputDirectory)
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(outputDirectory, "basisFunctions.csv")))
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
            e.printStackTrace();
        }
    }

    /**
     * Deserializes basis functions only.
     * Does not deserialize weights (which can be loaded as images) or diffuse basis colors (which should be re-fit, or a diffuse texture can be used instead).
     * @param priorSolutionDirectory
     * @return An object containing the red, green, and blue basis functions.
     */
    public static SpecularBasis deserializeBasisFunctions(int basisCount, int microfacetDistributionResolution, File priorSolutionDirectory)
    {
        SimpleSpecularBasis specularBasis = new SimpleSpecularBasis(basisCount, microfacetDistributionResolution);

        // Text file format
        try (Scanner in = new Scanner(new File(priorSolutionDirectory, "basisFunctions.csv")))
        {
            in.useDelimiter("\\s*[,\\n\\r]+\\s*"); // CSV

            for (int b = 0; b < basisCount; b++)
            {
                in.next(); // "Red#{b}"
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    specularBasis.setRed(b, m, in.nextDouble());
                }
                // newline

                in.next(); // "Green#{b}"
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    specularBasis.setGreen(b, m, in.nextDouble());
                }
                // newline

                in.next(); // "Blue#{b}"
                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    specularBasis.setBlue(b, m, in.nextDouble());
                }
                // newline
            }
            // newline
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        return specularBasis;
    }
}
