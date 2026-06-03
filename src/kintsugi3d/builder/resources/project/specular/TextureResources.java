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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public interface TextureResources<ContextType extends Context<ContextType>>
    extends AutoCloseable, ContextBound<ContextType>, Blittable<TextureResources<ContextType>>
{
    int WEIGHTS_PER_PACKED_CHANNEL = 4;

    Map<String, Texture2D<ContextType>> getTextures();

    /**
     * Returns a map containing only the standard textures
     * @return
     */
    default Map<StandardTexture, Texture2D<ContextType>> getStandardTextures()
    {
        return StandardTexture.convertStringMapToEnumMap(getTextures());
    }

    default Texture2D<ContextType> getTexture(String texName)
    {
        return getTextures().get(texName);
    }

    default Texture2D<ContextType> getTexture(StandardTexture tex)
    {
        return getTextures().get(tex.texName);
    }

    BasisResources<ContextType> getBasisResources();

    BasisWeightResources<ContextType> getBasisWeightResources();

    @Override
    void close(); // no exception

    private <SourceType extends Blittable<?>> void blitCroppedAndScaledSingle(
        Blittable<SourceType> destTex, int destX, int destY, int destWidth, int destHeight,
        TextureResources<ContextType> readSource, SourceType srcTex, int srcX, int srcY, int srcWidth, int srcHeight,
        boolean linearFiltering)
    {
        if (destTex != null && srcTex != null)
        {
            if (destTex.getWidth() == this.getWidth() && destTex.getHeight() == this.getHeight()
                && srcTex.getWidth() == readSource.getWidth() && srcTex.getHeight() == readSource.getHeight())
            {
                // dimensions match, so just do a normal blit
                destTex.blitCroppedAndScaled(destX, destY, destWidth, destHeight,
                    srcTex, srcX, srcY, srcWidth, srcHeight, linearFiltering);
            }
            else
            {
                // dimensions do not match; try to remap rectangles to grab the same relative area in each
                destTex.blitCroppedAndScaled(
                    (int) Math.round((double) destX * destTex.getWidth() / this.getWidth()),
                    (int) Math.round((double) destY * destTex.getHeight() / this.getHeight()),
                    (int) Math.round((double) destWidth * destTex.getWidth() / this.getWidth()),
                    (int) Math.round((double) destHeight * destTex.getHeight() / this.getHeight()),
                    srcTex,
                    (int) Math.round((double) srcX * srcTex.getWidth() / readSource.getWidth()),
                    (int) Math.round((double) srcY * srcTex.getHeight() / readSource.getHeight()),
                    (int) Math.round((double) srcWidth * srcTex.getWidth() / readSource.getWidth()),
                    (int) Math.round((double) srcHeight * srcTex.getHeight() / readSource.getHeight()),
                    linearFiltering);
            }
        }
    }

    /**
     * Copies pixels from part of a blittable to another.  The copying operation will be start at (x, y) within
     * this blittable, and resize if the requested source and destination rectangles are not the same size.
     *
     * @param destX           The left edge of the rectangle to copy into within this blittable.
     * @param destY           The bottom edge of the rectangle to copy into within this blittable.
     * @param destWidth       The width of the rectangle to copy at the destination resolution.
     * @param destHeight      The height of the rectangle to copy at the destination resolution.
     * @param readSource      The blittable source to copy from.
     * @param srcX            The left edge of the rectangle to copy from within the source.
     * @param srcY            The bottom edge of the rectangle to copy from within the source.
     * @param srcWidth        The width of the rectangle to copy at the source resolution.
     * @param srcHeight       The height of the rectangle to copy at the source resolution.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     */
    @Override
    default void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight,
                                      TextureResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
    {
        // Blit each individual texture -- i.e. diffuse, normal, specular reflectivity, specular roughness
        for (var texEntry : getTextures().entrySet())
        {
            if (readSource.getTextures().containsKey(texEntry.getKey())) // both source and destination must contain the texture to blit
            {
                blitCroppedAndScaledSingle(texEntry.getValue(), destX, destY, destWidth, destHeight,
                    readSource, readSource.getTexture(texEntry.getKey()), srcX, srcY, srcWidth, srcHeight, linearFiltering);
            }
        }

        // Blit weight maps, weight mask -- handled separately
        if (this.getBasisWeightResources() != null && readSource.getBasisWeightResources() != null)
        {
            blitCroppedAndScaledSingle(this.getBasisWeightResources().weightMaps, destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().weightMaps, srcX, srcY, srcWidth, srcHeight, linearFiltering);
            blitCroppedAndScaledSingle(this.getBasisWeightResources().weightMask, destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().weightMask, srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }
    }

    void setupShaderProgram(Program<ContextType> program);

    static <ContextType extends Context<ContextType>> TextureResources<ContextType> makeNull(ContextType context)
    {
        return new TextureResources<>()
        {
            @Override
            public ContextType getContext()
            {
                return context;
            }

            @Override
            public int getWidth()
            {
                return 0;
            }

            @Override
            public int getHeight()
            {
                return 0;
            }

            @Override
            public Map<String, Texture2D<ContextType>> getTextures()
            {
                return Map.of();
            }

            @Override
            public BasisResources<ContextType> getBasisResources()
            {
                return null;
            }

            @Override
            public BasisWeightResources<ContextType> getBasisWeightResources()
            {
                return null;
            }

            @Override
            public void close()
            {
            }

            @Override
            public void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight, TextureResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
            {
                // Do nothing
            }

            @Override
            public void setupShaderProgram(Program<ContextType> program)
            {
            }

            @Override
            public void saveTexture(String texName, String format, File outputDirectory, String filenameOverride)
            {
            }

            @Override
            public void savePackedWeightMaps(String format, File outputDirectory, String filenamePrefix)
            {
            }

            @Override
            public void saveUnpackedWeightMaps(String format, File outputDirectory, String filenamePrefix)
            {
            }

            @Override
            public void saveBasisFunctions(File outputDirectory, String filenameOverride)
            {
            }

            @Override
            public void deleteBasisMaterial(int materialIndex)
            {
            }
        };
    }

    /**
     * Saves a texture to the filesystem in the specified format.
     *
     * @param texName          The name of the texture to save.
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveTexture(String texName, String format, File outputDirectory, String filenameOverride);

    /**
     * Saves a texture to the filesystem in the specified format.
     *
     * @param tex              The texture to save.
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    default void saveTexture(StandardTexture tex, String format,  File outputDirectory, String filenameOverride)
    {
        saveTexture(tex.texName, format, outputDirectory, filenameOverride);
    }

    /**
     * Saves weight map textures to the filesystem in the specified format,
     * with four weight maps packed into a single image in the RGBA channels.
     *
     * @param format            The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory   The directory in which to save the textures.
     * @param filenamePrefix    A string to be prepended to each weightmap's filename.
     */
    void savePackedWeightMaps(String format, File outputDirectory, String filenamePrefix);

    /**
     * Saves packed weight map textures to the filesystem in the specified format
     * with four weight maps packed into a single image in the RGBA channels, using default filenames.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     */
    default void savePackedWeightMaps(String format, File outputDirectory)
    {
        savePackedWeightMaps(format, outputDirectory, "");
    }

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images.
     *
     * @param format            The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory   The directory in which to save the textures.
     * @param filenamePrefix    A string to be prepended to each weightmap's filename.
     */
    void saveUnpackedWeightMaps(String format, File outputDirectory, String filenamePrefix);

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images, using default filenames.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     */
    default void saveUnpackedWeightMaps(String format, File outputDirectory)
    {
        saveUnpackedWeightMaps(format, outputDirectory, "");
    }

    /**
     * Saves the basis function to the filesystem as a CSV file.
     *
     * @param outputDirectory  The directory in which to save the basis functions.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveBasisFunctions(File outputDirectory, String filenameOverride);

    /**
     * Saves basis function textures to the filesystem with a default filename.
     *
     * @param outputDirectory The directory in which to save the basis functions.
     */
    default void saveBasisFunctions(File outputDirectory)
    {
        saveBasisFunctions(outputDirectory, null);
    }

    static String getTextureFilename(StandardTexture tex, String format)
    {
        return getTextureFilename(tex.texName, format);
    }

    static String getTextureFilename(StandardTexture tex, String format, String filenamePrefix)
    {
        return getTextureFilename(tex.texName, format, filenamePrefix);
    }

    static String getTextureFilename(String texName, String format)
    {
        return getTextureFilename(texName, format, "");
    }

    static String getTextureFilename(String texName, String format, String filenamePrefix)
    {
        return String.format("%s%s.%s", filenamePrefix, texName, format.toLowerCase(Locale.ROOT));
    }

    static String getPackedWeightMapFilename(int index, String format)
    {
        return getPackedWeightMapFilename(index, format, "");
    }

    static String getPackedWeightMapFilename(int index, String format, String filenamePrefix)
    {
        return getTextureFilename(getPackedWeightMapName(index), format, filenamePrefix);
    }

    static String getPackedWeightMapName(int index)
    {
        int scaledWeightMapIndex = index * WEIGHTS_PER_PACKED_CHANNEL;
        return String.format("weights%02d%02d", scaledWeightMapIndex, scaledWeightMapIndex + (WEIGHTS_PER_PACKED_CHANNEL - 1));
    }

    static String getUnpackedWeightMapFilename(int index, String format)
    {
        return getUnpackedWeightMapFilename(index, format, "");
    }

    static String getUnpackedWeightMapFilename(int index, String format, String filenamePrefix)
    {
        return getTextureFilename(getUnpackedWeightMapName(index), format, filenamePrefix);
    }

    static String getUnpackedWeightMapName(int index)
    {
        return String.format("weights%02d", index);
    }

    static String getBasisFunctionsFilename()
    {
        return getBasisFunctionsFilename("");
    }

    static String getBasisFunctionsFilename(String filenamePrefix)
    {
        return String.format("%sbasisFunctions.csv", filenamePrefix);
    }

    /**
     * Saves the specified named textures to the filesystem as images in the specified format.
     * @param texNames        The names of the textures to save.
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the metadata maps.
     * @param filenamePrefix  A prefix to attach to each file (i.e. the name of the project).
     *                        This can be set to the empty string "" to use just the base / default names.
     */
    default void saveNamedTextures(Iterable<String> texNames, String format, File outputDirectory, String filenamePrefix)
    {
        for (String name : texNames)
        {
            saveTexture(name, format, outputDirectory, getTextureFilename(name, format, filenamePrefix));
        }
    }

    /**
     * Saves all named textures (but not weight maps) to the filesystem as images in the specified format.
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the metadata maps.
     * @param filenamePrefix  A prefix to attach to each file (i.e. the name of the project).
     *                        This can be set to the empty string "" to use just the base / default names.
     */
    default void saveAllNamedTextures(String format, File outputDirectory, String filenamePrefix)
    {
        saveNamedTextures(getTextures().keySet(), format, outputDirectory, filenamePrefix);
    }

    /**
     * Saves all resources to the specified output directory with the specified image format, using default filenames.
     * This includes all named textures, as well as basis functions and both packed and unpacked weight maps.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory
     */
    default void saveAll(String format, File outputDirectory)
    {
        saveAllNamedTextures(format, outputDirectory, "");
        saveBasisFunctions(outputDirectory, null);
        savePackedWeightMaps(format, outputDirectory, null);
        saveUnpackedWeightMaps(format, outputDirectory, null);
    }

    /**
     * Saves all resources to the specified output directory in PNG format using default filenames.
     *
     * @param outputDirectory
     */
    default void saveAll(File outputDirectory)
    {
        saveAll("PNG", outputDirectory);
    }

    private static File getTextureFile(File priorSolutionDirectory, StandardTexture t)
    {
        return getTextureFile(priorSolutionDirectory, t.texName);
    }

    private static File getTextureFile(File priorSolutionDirectory, String texName)
    {
        return new File(priorSolutionDirectory, getTextureFilename(texName, "PNG"));
    }

    static <ContextType extends Context<ContextType>>
    Texture2D<ContextType> loadTexture(String texName, File directory, ContextType context) throws IOException
    {
        // Load texture file
        File textureFile = getTextureFile(directory, texName);

        if (textureFile.exists())
        {
            return context.getTextureFactory()
                .build2DColorTextureFromFile(textureFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture();
        }
        else
        {
            return null;
        }
    }

    static <ContextType extends Context<ContextType>>
    Texture2D<ContextType> loadTexture(StandardTexture tex, File directory, ContextType context) throws IOException
    {
        return loadTexture(tex.texName, directory, context);
    }

    default Texture2D<ContextType> loadTexture(String texName, File directory) throws IOException
    {
        return loadTexture(texName, directory, getContext());
    }

    default Texture2D<ContextType> loadTexture(StandardTexture tex, File directory) throws IOException
    {
        return loadTexture(tex.texName, directory, getContext());
    }

    /**
     * Deletes one of the basis materials.
     * This will cause the basis materials, weight maps, and thumbnail images to be automatically re-saved
     * to the project's supporting files directory.
      * @param materialIndex
     */
    void deleteBasisMaterial(int materialIndex);
}
