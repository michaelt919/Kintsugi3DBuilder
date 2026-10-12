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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
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

    public static void saveWeightImages(MaterialBasis basis, int width, int height, SpecularBasisWeights basisWeights, File outputDirectory)
    {
        for (BasisMaterialInfo material : basis.getMaterials())
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];

            for (int p = 0; p < (width * height); p++)
            {
                float weight = (float)basisWeights.getWeight(material.getGPUIndex(), p);

                // Flip vertically
                int dataBufferIndex = (p % width) + (width * (height - (p / width) - 1));
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
     *
     * @param priorSolutionDirectory
     * @return An object containing the red, green, and blue basis functions.
     */
    public static MutableMaterialBasis deserializeBasisFunctions(File priorSolutionDirectory) throws IOException
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
                    String[] tagSplit = currentTag.split("#", 2);
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

    /// Serializes basis functions into an HDR image.
    /// DOES NOT SERIALIZE DIFFUSE COLORS OR DISABLED WEIGHT MAPS!
    ///
    /// @param basis                            the material basis in which to evaluate specular colors from
    /// @param outputDirectory                  where to put the file
    /// @param filenameOverride                 give the file a different name
    public static void serializeHDRI(MaterialBasis basis, File outputDirectory, String filenameOverride)
    {
        // Only put enabled materials in the HDRI, similar to packed weight maps
        int materialCount = basis.getEnabledMaterialCount();
        int resolution = basis.getSpecularResolution();

        // Calculate RGBE bytes
        byte[] rgbe = new byte[materialCount * resolution * 4];
        for (BasisMaterialInfo material : basis.getMaterials())
        {
            for (int m = 0; m < basis.getSpecularResolution(); ++m)
            {
                // Using GPU index so that the indexing matches how we save packed weight maps.
                System.arraycopy(doubleToRgbe(
                    material.evaluateSpecularRed(m), material.evaluateSpecularGreen(m), material.evaluateSpecularBlue(m)),
                    0, rgbe, ((material.getGPUIndex() * m) + m) * 4, 4);
            }
        }

        // Write the bytes out to the HDRI
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, Objects.requireNonNullElse(filenameOverride, "basisFunctions.hdr")))))
        {
            // Write HDR header
            out.write("#?RADIANCE\n".getBytes(StandardCharsets.US_ASCII));
            out.write("FORMAT=32-bit_rle_rgbe\n\n".getBytes(StandardCharsets.US_ASCII));
            out.write(String.format("-Y %d +X %d%n", materialCount, resolution).getBytes(StandardCharsets.US_ASCII));

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
     * Deserializes basis functions packed into an HDRI
     * Does not deserialize weights, diffuse basis colors, or albedo colors
     *
     * @param priorSolutionDirectory
     * @return An object containing the red, green, and blue basis functions.
     */
    public static MaterialBasis deserializeHDRI(File priorSolutionDirectory) throws IOException
    {
        File basisFile = new File(priorSolutionDirectory, "basisFunctions.hdr");

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
            StringBuilder builder = new StringBuilder(16);
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
            rgbe = in.readAllBytes();
        }

        // For final product
        List<double[]> specularRedBasis = new ArrayList<>(basisCount);
        List<double[]> specularGreenBasis = new ArrayList<>(basisCount);
        List<double[]> specularBlueBasis = new ArrayList<>(basisCount);

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

        // Default all to black
        DoubleVector3[] diffuse = new DoubleVector3[basisCount];
        Arrays.fill(diffuse, DoubleVector3.ZERO);

        return new SimpleMaterialBasis(diffuse, specularRedBasis, specularGreenBasis, specularBlueBasis);
    }

    private static double[] rgbeToDouble(byte[] rgbe)
    {
        double[] rgb = new double[3];
        // bitwise operation is for "unsigning" bytes
        if ((rgbe[3] & 0xFF) > 0)
        { // If exponent is 0, pixel is pure black
            // 2^(exponent - Radiance bias - 8) or 2^(exp - bias) / 256 to get a range from "0 to 1"
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
