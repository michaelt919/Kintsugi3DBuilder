/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.specular;

import kintsugi3d.builder.fit.decomposition.*;
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
import java.util.*;
import java.util.regex.Pattern;

public final class SpecularFitSerializer
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitSerializer.class);
    private static final Pattern CSV_PATTERN = Pattern.compile("\\s*,+\\s*");

    private SpecularFitSerializer()
    {
    }

    public static void saveWeightImages(MaterialBasis basis, int width, int height, SpecularBasisWeights basisWeights, File outputDirectory)
    {
        for (BasisMaterialInfo material : basis.getMaterials())
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];

            for (int p = 0; p < width * height; p++)
            {
                float weight = (float)basisWeights.getWeight(material.getGPUIndex(), p);

                // Flip vertically
                int dataBufferIndex = p % width + width * (height - p / width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG",
                    new File(outputDirectory, BasisWeightResources.getUnpackedWeightMapFilename(material.getName())));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving weight images:", e);
            }
        }
    }

    public static void serializeBasisFunctions(
        int microfacetDistributionResolution, MaterialBasis basis, File outputDirectory, String filenameOverride)
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(outputDirectory,
            filenameOverride != null ? filenameOverride : BasisResources.getBasisFunctionsFilename()), StandardCharsets.UTF_8))
        {
            for (BasisMaterialInfo material : basis.getMaterials())
            {
                boolean isEnabled = material.isEnabled();
                String name = material.getName();

                // Red
                if (isEnabled)
                {
                    out.printf("Red#%s", name);
                }
                else
                {
                    out.printf("RedDisabled#%s", name);
                }

                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(material.evaluateSpecularRed(m));
                }

                out.println();

                // Green
                if (isEnabled)
                {
                    out.printf("Green#%s", name);
                }
                else
                {
                    out.printf("GreenDisabled#%s", name);
                }

                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(material.evaluateSpecularGreen(m));
                }

                out.println();

                // Blue
                if (isEnabled)
                {
                    out.printf("Blue#%s", name);
                }
                else
                {
                    out.printf("BlueDisabled#%s", name);
                }

                for (int m = 0; m <= microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(material.evaluateSpecularBlue(m));
                }

                out.println();
            }

            // Write diffuse last for consistency with prior versions
            // (Kintsugi 3D Viewer does not support other basis functions listed after for diffuse colors)
            for (BasisMaterialInfo material : basis.getMaterials())
            {
                String name = material.getName();

                // Diffuse
                DoubleVector3 diffuseColor = material.getDiffuseColor();
                if (material.isEnabled())
                {
                    out.printf("Diffuse#%s, %f, %f, %f", name, diffuseColor.x, diffuseColor.y, diffuseColor.z);
                }
                else
                {
                    out.printf("DiffuseDisabled#%s, %f, %f, %f", name, diffuseColor.x, diffuseColor.y, diffuseColor.z);
                }

                out.println();
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving basis functions:", e);
        }
    }

    /**
     * Deserializes basis functions only.
     * Does not deserialize weights (which can be loaded as images) or diffuse basis colors (which should be re-fit, or a diffuse texture can be used instead).
     * @param priorSolutionDirectory
     * @return An object containing the red, green, and blue basis functions.
     */
    public static MutableMaterialBasis deserializeBasisFunctions(File priorSolutionDirectory)
        throws IOException
    {
        File basisFile = new File(priorSolutionDirectory, BasisResources.getBasisFunctionsFilename());

        if (basisFile.exists())
        {
            // Test to figure out the resolution
            int numElements; // Technically this is "microfacetDistributionResolution + 1" the way it's defined elsewhere
            try (Scanner in = new Scanner(basisFile, StandardCharsets.UTF_8))
            {
                in.useLocale(Locale.ROOT);
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
                in.useLocale(Locale.ROOT);

                Collection<String> names = new LinkedHashSet<>(8); // preserve insertion order
                Collection<String> disabledNames = new LinkedHashSet<>(8);

                Map<String, double[]> specularRedBasis = new HashMap<>(8);
                Map<String, double[]> specularGreenBasis = new HashMap<>(8);
                Map<String, double[]> specularBlueBasis = new HashMap<>(8);
                Map<String, DoubleVector3> diffuseBasis = new HashMap<>(8);

                in.useDelimiter("\\s*[,\\n\\r]+\\s*"); // CSV

                String currentTag = in.next();
                while (in.hasNext()) // stop at end of file
                {
                    String[] tagSplit = currentTag.split("#", 1);
                    String tagType = tagSplit[0];
                    String name = tagSplit[1];

                    if ("Diffuse".equals(tagType))
                    {
                        names.add(name);
                        disabledNames.remove(name); // Ensure that a name is not in both lists.
                        diffuseBasis.put(name, new DoubleVector3(in.nextDouble(), in.nextDouble(), in.nextDouble()));
                    }
                    else if ("DiffuseDisabled".equals(tagType))
                    {
                        disabledNames.add(name);
                        names.remove(name); // Ensure that a name is not in both lists.
                        diffuseBasis.put(name, new DoubleVector3(in.nextDouble(), in.nextDouble(), in.nextDouble()));
                    }
                    else
                    {
                        // Tags which require an array of basis elements
                        if ("Red".equals(tagType))
                        {
                            specularRedBasis.put(name, readBasisLine(in, numElements));
                            names.add(name);
                            disabledNames.remove(name); // Ensure that a name is not in both lists.
                        }
                        else if ("RedDisabled".equals(tagType))
                        {
                            specularRedBasis.put(name, readBasisLine(in, numElements));
                            disabledNames.add(name);
                            names.remove(name); // Ensure that a name is not in both lists.
                        }
                        else if ("Green".equals(tagType))
                        {
                            specularGreenBasis.put(name, readBasisLine(in, numElements));
                            names.add(name);
                            disabledNames.remove(name); // Ensure that a name is not in both lists.
                        }
                        else if ("GreenDisabled".equals(tagType))
                        {
                            specularGreenBasis.put(name, readBasisLine(in, numElements));
                            disabledNames.add(name);
                            names.remove(name); // Ensure that a name is not in both lists.
                        }
                        else if ("Blue".equals(tagType))
                        {
                            specularBlueBasis.put(name, readBasisLine(in, numElements));
                            names.add(name);
                            disabledNames.remove(name); // Ensure that a name is not in both lists.
                        }
                        else if ("BlueDisabled".equals(tagType))
                        {
                            specularBlueBasis.put(name, readBasisLine(in, numElements));
                            disabledNames.add(name);
                            names.remove(name); // Ensure that a name is not in both lists.
                        }
                        else
                        {
                            throw new IOException(MessageFormat.format("Unexpected line beginning with {0}", currentTag));
                        }
                    }

                    if (in.hasNext())
                    {
                        // Get tag of next element for while loop check
                        currentTag = in.next();
                    }
                }

                for (String name : names)
                {
                    // Default to black if not found
                    if (!specularRedBasis.containsKey(name))
                    {
                        specularRedBasis.put(name, new double[numElements]);
                    }

                    if (!specularGreenBasis.containsKey(name))
                    {
                        specularGreenBasis.put(name, new double[numElements]);
                    }

                    if (!specularBlueBasis.containsKey(name))
                    {
                        specularBlueBasis.put(name, new double[numElements]);
                    }

                    if (!diffuseBasis.containsKey(name))
                    {
                        diffuseBasis.put(name, DoubleVector3.ZERO);
                    }
                }

                for (String name : disabledNames)
                {
                    // Default to black if not found
                    if (!specularRedBasis.containsKey(name))
                    {
                        specularRedBasis.put(name, new double[numElements]);
                    }

                    if (!specularGreenBasis.containsKey(name))
                    {
                        specularGreenBasis.put(name, new double[numElements]);
                    }

                    if (!specularBlueBasis.containsKey(name))
                    {
                        specularBlueBasis.put(name, new double[numElements]);
                    }

                    if (!diffuseBasis.containsKey(name))
                    {
                        diffuseBasis.put(name, DoubleVector3.ZERO);
                    }
                }

                return new SimpleMaterialBasis(nameSetToMap(names), nameSetToMap(disabledNames), diffuseBasis,
                    specularRedBasis, specularGreenBasis, specularBlueBasis
                );
            }
        }
        else
        {
            return null;
        }
    }

    private static double[] readBasisLine(Scanner in, int numElements)
    {
        double[] newBasis = new double[numElements];
        Arrays.setAll(newBasis, m -> in.nextDouble());
        return newBasis;
    }

    private static Map<String, String> nameSetToMap(Collection<String> set)
    {
        Map<String, String> map = new HashMap<>(set.size());
        for (String name : set)
        {
            map.put(name, name);
        }
        return map;
    }
}
