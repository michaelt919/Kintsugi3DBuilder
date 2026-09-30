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

import kintsugi3d.builder.core.texture.NamedTextureInfo;
import kintsugi3d.builder.core.texture.StandardTexture;
import kintsugi3d.builder.core.texture.TextureInfo;
import kintsugi3d.builder.fit.decomposition.BasisMaterialInfo;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.fit.decomposition.MutableBasisResources;
import kintsugi3d.builder.util.MappedChange;
import kintsugi3d.builder.util.Observable;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public interface TextureResources<ContextType extends Context<ContextType>>
    extends Blittable<ReadonlyTextureResources<ContextType>>, ReadonlyTextureResources<ContextType>, ManagedResource
{
    int WEIGHTS_PER_CHANNEL_PACKED_IMAGE = 4;

    @Override
    Map<TextureInfo, ? extends Texture2D<ContextType>> getTextures();

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
        return getTextures().get(new NamedTextureInfo(texName));
    }

    @Override
    default Texture2D<ContextType> getTexture(TextureInfo tex)
    {
        return getTextures().get(tex);
    }

    @Override
    default Texture2D<ContextType> getTexture(StandardTexture tex)
    {
        return getTextures().get(tex.details);
    }

    void disableBasisMaterials(Collection<String> materialNames);

    void enableBasisMaterials(Collection<String> materialNames);

    void toggleBasisMaterial(String materialName);

    /**
     * Deletes one of the basis materials.
     * This will cause the basis materials, weight maps, and thumbnail images to be automatically re-saved
     * to the project's supporting files directory.
      * @param materialName
     */
    void deleteBasisMaterial(String materialName);

    /**
     * Refreshes a texture specified by key using the default location for the given texture.
     * @param key The TextureDetails used to choose which texture to refresh.
     * @param parentDirectory
     * @throws IOException
     */
    void replaceTextureWithDefaultFile(TextureInfo key, File parentDirectory) throws IOException;

    /**
     * Replaces a texture by key with a specific file.
     * @param key
     * @param newTextureFile
     * @throws IOException
     */
    void replaceTextureWithSpecificFile(TextureInfo key, File newTextureFile) throws IOException;

    /**
     * Refreshes a weightmap specified by its associated basis material using the default location for the given texture.
     * @param material The basis material associated with the texture to refresh.
     * @param parentDirectory
     * @throws IOException
     */
    void replaceWeightMapWithDefaultFile(BasisMaterialInfo material, File parentDirectory) throws IOException;

    /**
     * Replaces a weightmap specified by its associated basis material with a specific file.
     * @param material The basis material associated with the texture to refresh.
     * @param newTextureFile
     * @throws IOException
     */
    void replaceWeightMapWithSpecificFile(BasisMaterialInfo material, File newTextureFile) throws IOException;

    /**
     * Refreshes all textures (including weightmaps) using the default location for each texture.
     * @param parentDirectory
     * @throws IOException
     */
    void replaceAllTexturesWithDefaultFiles(File parentDirectory) throws IOException;

    void setBasisObservable(Observable<MappedChange<String, BasisMaterialInfo>> basisObservable);

    void setTexturesObservable(Observable<MappedChange<String, TextureInfo>> texturesObservable);

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

    static String getTextureFilename(String texName)
    {
        return getTextureFilename(texName, "PNG");
    }

    static String getTextureFilename(String texName, String format, String filenamePrefix)
    {
        return String.format("%s%s.%s", filenamePrefix, texName, format.toLowerCase(Locale.ROOT));
    }

    static String getPackedWeightMapFilename(int index, String format, String filenamePrefix)
    {
        return getTextureFilename(getPackedWeightMapName(index), format, filenamePrefix);
    }

    static String getPackedWeightMapFilename(int index, String format)
    {
        return getPackedWeightMapFilename(index, format, "");
    }

    static String getPackedWeightMapFilename(int index)
    {
        return getPackedWeightMapFilename(index, "PNG");
    }

    static String getPackedWeightMapName(int index)
    {
        int scaledWeightMapIndex = index * WEIGHTS_PER_CHANNEL_PACKED_IMAGE;
        return String.format("weights%02d%02d", scaledWeightMapIndex, scaledWeightMapIndex + (WEIGHTS_PER_CHANNEL_PACKED_IMAGE - 1));
    }

    static File getTextureFile(StandardTexture t, File directory)
    {
        return getTextureFile(t.details.name, directory);
    }

    static File getTextureFile(String texName, File directory)
    {
        return new File(directory, getTextureFilename(texName));
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

    default Texture2D<ContextType> loadTexture(String texName, File directory) throws IOException
    {
        return loadTexture(texName, directory, getContext());
    }

    default Texture2D<ContextType> loadTexture(StandardTexture tex, File directory) throws IOException
    {
        return loadTexture(tex.details.name, directory, getContext());
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
            public Map<TextureInfo, Texture2D<ContextType>> getTextures()
            {
                return Map.of();
            }

            @Override
            public MutableBasisResources<ContextType> getBasisResources()
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
            public void blitCroppedAndScaled(
                int destX, int destY, int destWidth, int destHeight,
                ReadonlyTextureResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight,
                boolean linearFiltering)
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
            public void saveUnpackedWeightMaps(String format, File outputDirectory)
            {
            }

            @Override
            public void saveBasisFunctions(File outputDirectory, String filenameOverride)
            {
            }
            @Override
            public void replaceTextureWithSpecificFile(TextureInfo key, File newTextureFile)
            {
            }

            @Override
            public void replaceTextureWithDefaultFile(TextureInfo key, File parentDirectory)
            {
            }

            @Override
            public void replaceWeightMapWithDefaultFile(BasisMaterialInfo material, File parentDirectory)
            {
            }

            @Override
            public void replaceWeightMapWithSpecificFile(BasisMaterialInfo material, File newTextureFile)
            {
            }

            @Override
            public void replaceAllTexturesWithDefaultFiles(File parentDirectory)
            {
            }


            @Override
            public void toggleBasisMaterial(String materialName)
            {
            }

            @Override
            public void disableBasisMaterials(Collection<String> materialNames)
            {
            }

            @Override
            public void enableBasisMaterials(Collection<String> materialNames)
            {
            }

            @Override
            public void deleteBasisMaterial(String materialName)
            {
            }

            @Override
            public void setBasisObservable(Observable<MappedChange<String, BasisMaterialInfo>> basisObservable)
            {
            }

            @Override
            public void setTexturesObservable(Observable<MappedChange<String, TextureInfo>> texturesObservable)
            {
            }
        };
    }
}
