/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.reflectancefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import javax.imageio.ImageIO;

import umn.gl.core.Framebuffer;
import umn.gl.core.FramebufferSize;

/**
 * A container for the results of a reflectance parameter estimation attempt.
 */
@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
public final class ParameterFittingResult
{
    private final float[] diffuseColorDataRGBA;
    private final float[] normalDataRGBA;
    private final float[] specularColorDataRGBA;
    private final float[] roughnessDataRGBA;
    private final float[] roughnessErrorDataRGBA;

    private final int[] diffuseColorPackedIntRGBA;
    private final int[] normalPackedIntRGBA;
    private final int[] specularColorPackedIntRGBA;
    private final int[] roughnessPackedIntRGBA;
    private final int[] roughnessErrorPackedIntRGBA;

    private final FramebufferSize framebufferSize;
    private final Options options;

    private ParameterFittingResult(Framebuffer<?> framebuffer, Options options)
    {
        framebufferSize = framebuffer.getSize();
        this.options = options;

        diffuseColorDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(0);
        normalDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(1);
        specularColorDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(2);
        roughnessDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(3);
        roughnessErrorDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(4);

        diffuseColorPackedIntRGBA = framebuffer.readColorBufferARGB(0);
        normalPackedIntRGBA = framebuffer.readColorBufferARGB(1);
        specularColorPackedIntRGBA = framebuffer.readColorBufferARGB(2);
        roughnessPackedIntRGBA = framebuffer.readColorBufferARGB(3);
        roughnessErrorPackedIntRGBA = framebuffer.readColorBufferARGB(4);
    }

    /**
     * Initializes a result from a framebuffer containing the results in graphics memory.
     * @param framebuffer The framebuffer containing the results.
     * @param options The options that were used to generate the reflectance parameter fitting results.
     * @return A new instance of ParameterFittingResult providing access to the results.
     */
    static ParameterFittingResult fromFramebuffer(Framebuffer<?> framebuffer, Options options)
    {
        return new ParameterFittingResult(framebuffer, options);
    }

    private void writeMTLFile(File mtlFile, String materialName) throws FileNotFoundException
    {
        //noinspection ImplicitDefaultCharsetUsage
        try(PrintStream materialStream = new PrintStream(mtlFile))
        {
            materialStream.println("newmtl " + materialName);

            if (options.isDiffuseTextureEnabled())
            {
                materialStream.println("Kd 1.0 1.0 1.0");
                materialStream.println("map_Kd " + materialName + "_Kd.png");
                materialStream.println("Ka 1.0 1.0 1.0");
                materialStream.println("map_Ka " + materialName + "_Kd.png");
            }
            else
            {
                materialStream.println("Kd 0.0 0.0 0.0");

                if (options.isSpecularTextureEnabled())
                {
                    materialStream.println("Ka 1.0 1.0 1.0");
                    materialStream.println("map_Ka " + materialName + "_Ks.png");
                }
                else
                {
                    materialStream.println("Ka 0 0 0");
                }
            }

            if (options.isSpecularTextureEnabled())
            {
                materialStream.println("Ks 1.0 1.0 1.0");
                materialStream.println("map_Ks " + materialName + "_Ks.png");

                materialStream.println("Pr 1.0");
                materialStream.println("map_Pr " + materialName + "_Pr.png");
            }
            else
            {
                materialStream.println("Ks 0 0 0");
            }

            if ((options.isDiffuseTextureEnabled() && options.getDiffuseComputedNormalWeight() > 0) || options.isSpecularTextureEnabled())
            {
                materialStream.println("norm " + materialName + "_norm.png");
            }

            materialStream.println("d 1.0");
            materialStream.println("Tr 0.0");
            materialStream.println("illum 5");

            materialStream.flush();
        }
    }

    private void writeTextureDataToOutputFile(int[] textureData, File outputFile) throws IOException
    {
        // Flip the array vertically
        FramebufferSize size = this.framebufferSize;
        for (int y = 0; y < size.height / 2; y++)
        {
            int limit = (y + 1) * size.width;
            for (int i1 = y * size.width, i2 = (size.height - y - 1) * size.width; i1 < limit; i1++, i2++)
            {
                int tmp = textureData[i1];
                textureData[i1] = textureData[i2];
                textureData[i2] = tmp;
            }
        }

        BufferedImage outImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, size.width, size.height, textureData, 0, size.width);
        ImageIO.write(outImg, "PNG", outputFile);
    }

    /**
     * Writes the results of the reflectance parameter estimation attempt to files on the hard drive.
     * @param materialFile The name of the material file to write.
     *                     This file contains references to reflectance parameter texture files that will be written as siblings of the material file
     *                     in the same parent directory.
     * @param materialName The name of the material to use when writing the material file.  This will also be used as a prefix for each of the
     *                     reflectance parameter texture files.
     * @throws IOException Thrown if an error occurs while writing files.
     */
    public void writeToFiles(File materialFile, String materialName) throws IOException
    {
        File outputDirectory = materialFile.getParentFile();
        outputDirectory.mkdirs();

        writeMTLFile(materialFile, materialName);

        if (options.isDiffuseTextureEnabled())
        {
            writeTextureDataToOutputFile(diffuseColorPackedIntRGBA, new File(outputDirectory, materialName + "_Kd.png"));
        }

        if ((options.isDiffuseTextureEnabled() && options.getDiffuseComputedNormalWeight() > 0) || options.isSpecularTextureEnabled())
        {
            writeTextureDataToOutputFile(normalPackedIntRGBA, new File(outputDirectory, materialName + "_norm.png"));
        }

        if (options.isSpecularTextureEnabled())
        {
            writeTextureDataToOutputFile(specularColorPackedIntRGBA, new File(outputDirectory, materialName + "_Ks.png"));
            writeTextureDataToOutputFile(roughnessPackedIntRGBA, new File(outputDirectory, materialName + "_Pr.png"));
            writeTextureDataToOutputFile(roughnessErrorPackedIntRGBA, new File(outputDirectory, materialName + "_Pr_error.png"));
        }
    }

    /**
     * Gets an array containing a flattened list of diffuse albedo estimates in a floating-point representation.
     * Data is stored in the RGBA format, meaning that a group of four floating-point values in the array corresponds to a single pixel in the diffuse
     * albedo image.  In other words, for a given pixel k, the red component is stored at index 4 * k, the green is stored at index 4 * k + 1,
     * the blue is stored at index 4 * k + 2, and the alpha is stored at index 4 * k + 3.
     * Pixels are arranged in row-major order, with rows at the bottom of the texture map occurring before rows at the top of the texture map.
     * @return An array containing the RGBA diffuse albedo data.
     */
    public float[] getDiffuseColorDataRGBA()
    {
        return diffuseColorDataRGBA;
    }

    /**
     * Gets an array containing a flattened list of surface normal estimates in a floating-point representation.
     * Data is stored in the RGBA format, meaning that a group of four floating-point values in the array corresponds to a single pixel in the normal
     * map image.  In other words, for a given pixel k, the red component is stored at index 4 * k, the green is stored at index 4 * k + 1,
     * the blue is stored at index 4 * k + 2, and the alpha is stored at index 4 * k + 3.
     * For a normal map, the normal is represented in tangent space, where the red component represents the projection of the normal onto the
     * tangent direction defined by the triangle mesh, the green component represents the projection of the normal onto the bitangent direction,
     * and the blue component represents the projection of the estimated normal onto the surface normal defined by the triangle mesh.
     * In each case, the range [-1, 1] is mapped onto the [0, 255] range of pixel values.
     * Pixels are arranged in row-major order, with rows at the bottom of the texture map occurring before rows at the top of the texture map.
     * @return An array containing the surface normal data.
     */
    public float[] getNormalDataRGBA()
    {
        return normalDataRGBA;
    }

    /**
     * Gets an array containing a flattened list of specular reflectivity estimates in a floating-point representation.
     * Data is stored in the RGBA format, meaning that a group of four floating-point values in the array corresponds to a single pixel in the specular
     * reflectivity image.  In other words, for a given pixel k, the red component is stored at index 4 * k, the green is stored at index 4 * k + 1,
     * the blue is stored at index 4 * k + 2, and the alpha is stored at index 4 * k + 3.
     * Pixels are arranged in row-major order, with rows at the bottom of the texture map occurring before rows at the top of the texture map.
     * @return An array containing the RGBA specular reflectivity data.
     */
    public float[] getSpecularColorDataRGBA()
    {
        return specularColorDataRGBA;
    }

    /**
     * Gets an array containing a flattened list of roughness estimates in a floating-point representation.
     * Data is stored in the RGBA format, meaning that a group of four floating-point values in the array corresponds to a single pixel in the
     * roughness image.  Since roughness is monochrome, this means that for a given pixel k, the red component (stored at index 4 * k), the green
     * (stored at index 4 * k + 1), and the blue (stored at index 4 * k + 2) should have the same value.
     * Pixels are arranged in row-major order, with rows at the bottom of the texture map occurring before rows at the top of the texture map.
     * @return An array containing the RGBA roughness data.
     */
    public float[] getRoughnessDataRGBA()
    {
        return roughnessDataRGBA;
    }

    /**
     * Gets an array containing a flattened list of specular reflectivity estimates in a floating-point representation.
     * Data is stored in the RGBA format, meaning that a group of four floating-point values in the array corresponds to a single pixel in the
     * roughness error image.  In other words, for a given pixel k, the red component is stored at index 4 * k, the green is stored at
     * index 4 * k + 1, the blue is stored at index 4 * k + 2, and the alpha is stored at index 4 * k + 3.
     * Although roughness is monochrome, each color channel may have a different error, so these three components will not necessarily all be the same.
     * Pixels are arranged in row-major order, with rows at the bottom of the texture map occurring before rows at the top of the texture map.
     * @return An array containing the RGBA roughness error data.
     */
    public float[] getRoughnessErrorDataRGBA()
    {
        return roughnessErrorDataRGBA;
    }
}
