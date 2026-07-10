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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.core.TextureDetails;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public interface TextureResources<ContextType extends Context<ContextType>>
    extends Blittable<TextureResources<ContextType>>, ReadonlyTextureResources<ContextType>, ManagedResource
{
    int WEIGHTS_PER_PACKED_CHANNEL = 4;

    Logger LOG = LoggerFactory.getLogger(TextureResources.class);

    @Override
    Map<TextureDetails, ? extends Texture2D<ContextType>> getTextures();

    /**
     * Returns a map containing only the standard textures
     * @return
     */
    @Override
    default Map<StandardTexture, ? extends Texture2D<ContextType>> getStandardTextures()
    {
        return StandardTexture.convertObjectMapToEnumMap(getTextures());
    }

    @Override
    default Texture2D<ContextType> getTexture(String texName)
    {
        return getTextures().get(new TextureDetails(texName));
    }

    @Override
    default Texture2D<ContextType> getTexture(TextureDetails tex)
    {
        return getTextures().get(tex);
    }

    @Override
    default Texture2D<ContextType> getTexture(StandardTexture tex)
    {
        return getTextures().get(tex.details);
    }

    @Override
    BasisResources<ContextType> getBasisResources();

    @Override
    BasisWeightResources<ContextType> getBasisWeightResources();

    private <SourceType extends TwoDimensional> void blitCroppedAndScaledSingle(
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
            blitCroppedAndScaledSingle(this.getBasisWeightResources().getWeightMaps(), destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().getWeightMaps(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
            blitCroppedAndScaledSingle(this.getBasisWeightResources().getWeightMask(), destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().getWeightMask(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }
    }

    default Texture2D<ContextType> loadTexture(String texName, File directory) throws IOException
    {
        return loadTexture(texName, directory, getContext());
    }

    default Texture2D<ContextType> loadTexture(StandardTexture tex, File directory) throws IOException
    {
        return loadTexture(tex.details.name, directory, getContext());
    }

    /**
     * Deletes one of the basis materials.
     * This will cause the basis materials, weight maps, and thumbnail images to be automatically re-saved
     * to the project's supporting files directory.
     * @param materialIndex
     */
    void deleteBasisMaterial(int materialIndex);

    /**
     * Refreshes a texture specified by key using the default location for the given texture.
     * @param key The TextureDetails used to choose which texture to refresh.
     * @param parentDirectory
     * @throws IOException
     */
    default void replaceTextureWithDefaultFile(TextureDetails key, File parentDirectory) throws IOException
    {
        getTextures().get(key).load(new File(parentDirectory, key.name + ".png"), true);
    }

    /**
     * Replaces a texture by key with a specific file.
     * @param key
     * @param newTextureFile
     * @throws IOException
     */
    default void replaceTextureWithSpecificFile(TextureDetails key, File newTextureFile) throws IOException
    {
        getTextures().get(key).load(newTextureFile, true);
    }

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
            public Map<TextureDetails, Texture2D<ContextType>> getTextures()
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

    static String getTextureFilename(StandardTexture tex, String format)
    {
        return getTextureFilename(tex.details.name, format);
    }

    static String getTextureFilename(StandardTexture tex, String format, String filenamePrefix)
    {
        return getTextureFilename(tex.details.name, format, filenamePrefix);
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

    static File getTextureFile(StandardTexture t, File directory)
    {
        return getTextureFile(t.details.name, directory);
    }

    static File getTextureFile(String texName, File directory)
    {
        return new File(directory, getTextureFilename(texName, "PNG"));
    }

    static <ContextType extends Context<ContextType>>
    Texture2D<ContextType> loadTexture(String texName, File directory, ContextType context) throws IOException
    {
        // Load texture file
        File textureFile = getTextureFile(texName, directory);

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
        return loadTexture(tex.details.name, directory, context);
    }
}
