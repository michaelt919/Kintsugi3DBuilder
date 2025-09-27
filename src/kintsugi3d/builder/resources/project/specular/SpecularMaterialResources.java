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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.util.Map;
import java.util.function.IntFunction;

public interface SpecularMaterialResources<ContextType extends Context<ContextType>>
    extends AutoCloseable, ContextBound<ContextType>, Blittable<SpecularMaterialResources<ContextType>>
{
    Texture2D<ContextType> getDiffuseMap();

    Texture2D<ContextType> getNormalMap();

    Texture2D<ContextType> getConstantMap();

//    Texture2D<ContextType> getQuadraticMap();

    Texture2D<ContextType> getSpecularReflectivityMap();

    Texture2D<ContextType> getSpecularRoughnessMap();

    Texture2D<ContextType> getAlbedoMap();

    Texture2D<ContextType> getORMMap();

    default Texture2D<ContextType> getOcclusionMap()
    {
        // Occlusion would be in red channel so ORM can be used in place of occlusion if no explicit occlusion map
        return getORMMap();
    }

    default Map<String, Texture2D<ContextType>> getMetadataMaps()
    {
        return Map.of();
    }

    BasisResources<ContextType> getBasisResources();

    BasisWeightResources<ContextType> getBasisWeightResources();

    @Override
    void close(); // no exception

    private <SourceType extends Blittable<?>> void blitCroppedAndScaledSingle(
        Blittable<SourceType> destTex, int destX, int destY, int destWidth, int destHeight,
        SpecularMaterialResources<ContextType> readSource, SourceType srcTex, int srcX, int srcY, int srcWidth, int srcHeight,
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
        SpecularMaterialResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
    {
        // Blit each individual texture -- diffuse, normal, specular reflectivity, specular roughness, weight maps, weight mask
        blitCroppedAndScaledSingle(this.getDiffuseMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getDiffuseMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getNormalMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getNormalMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getConstantMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getConstantMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
//        blitCroppedAndScaledSingle(this.getQuadraticMap(), destX, destY, destWidth, destHeight,
//            readSource, readSource.getQuadraticMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getSpecularReflectivityMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getSpecularReflectivityMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getSpecularRoughnessMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getSpecularRoughnessMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getAlbedoMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getAlbedoMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getORMMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getORMMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);

        if (this.getOcclusionMap() != this.getORMMap())
        {
            // Occlusion map and ORM map may be the same; so only blit if they are distinct in the destination
            // to prevent overwriting ORM with occlusion
            blitCroppedAndScaledSingle(this.getOcclusionMap(), destX, destY, destWidth, destHeight,
                readSource, readSource.getOcclusionMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }

        if (this.getBasisWeightResources() != null && readSource.getBasisWeightResources() != null)
        {
            blitCroppedAndScaledSingle(this.getBasisWeightResources().weightMaps, destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().weightMaps, srcX, srcY, srcWidth, srcHeight, linearFiltering);
            blitCroppedAndScaledSingle(this.getBasisWeightResources().weightMask, destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().weightMask, srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }

        Map<String, Texture2D<ContextType>> metadataMaps = this.getMetadataMaps();
        for (var entry : metadataMaps.entrySet())
        {
            Map<String, Texture2D<ContextType>> sourceMetadataMaps = readSource.getMetadataMaps();
            if (sourceMetadataMaps.containsKey(entry.getKey())) // both source and destination must contain the metadata map to blit
            {
                blitCroppedAndScaledSingle(entry.getValue(), destX, destY, destWidth, destHeight,
                    readSource, sourceMetadataMaps.get(entry.getKey()), srcX, srcY, srcWidth, srcHeight, linearFiltering);
            }
        }
    }

    void setupShaderProgram(Program<ContextType> program);

    static <ContextType extends Context<ContextType>> SpecularMaterialResources<ContextType> makeNull(ContextType context)
    {
        return new SpecularMaterialResources<>()
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
            public Texture2D<ContextType> getDiffuseMap()
            {
                return null;
            }

            @Override
            public Texture2D<ContextType> getNormalMap()
            {
                return null;
            }

            @Override
            public Texture2D<ContextType> getConstantMap()
            {
                return null;
            }

            @Override
            public Texture2D<ContextType> getSpecularReflectivityMap()
            {
                return null;
            }

            @Override
            public Texture2D<ContextType> getSpecularRoughnessMap()
            {
                return null;
            }

            @Override
            public Texture2D<ContextType> getAlbedoMap()
            {
                return null;
            }

            @Override
            public Texture2D<ContextType> getORMMap()
            {
                return null;
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
            public void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight, SpecularMaterialResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
            {
                // Do nothing
            }

            @Override
            public void setupShaderProgram(Program<ContextType> program)
            {
            }

            @Override
            public void saveDiffuseMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveNormalMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveSpecularReflectivityMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveSpecularRoughnessMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveConstantMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveOcclusionMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveAlbedoMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void saveORMMap(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void savePackedWeightMaps(String format, File outputDirectory, IntFunction<String> filenameOverrides)
            {
            }

            @Override
            public void saveUnpackedWeightMaps(String format, File outputDirectory, IntFunction<String> filenameOverrides)
            {
            }

            @Override
            public void saveBasisFunctions(File outputDirectory, String filenamePrefix)
            {
            }

            @Override
            public void saveMetadataMaps(String format, File outputDirectory, String filename)
            {
            }

            @Override
            public void deleteBasisMaterial(int materialIndex)
            {
            }
        };
    }

    /**
     * Saves the diffuse texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveDiffuseMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the diffuse texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveDiffuseMap(String format, File outputDirectory)
    {
        saveDiffuseMap(format, outputDirectory, null);
    }

    /**
     * Saves the normal map texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveNormalMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the normal map texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveNormalMap(String format, File outputDirectory)
    {
        saveNormalMap(format, outputDirectory, null);
    }

    /**
     * Saves the specular reflectivity texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveSpecularReflectivityMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the specular reflectivity texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveSpecularReflectivityMap(String format, File outputDirectory)
    {
        saveSpecularReflectivityMap(format, outputDirectory, null);
    }

    /**
     * Saves the specular roughness texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveSpecularRoughnessMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the specular roughness texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveSpecularRoughnessMap(String format, File outputDirectory)
    {
        saveSpecularRoughnessMap(format, outputDirectory, null);
    }

    /**
     * Saves the constant (translucency) texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveConstantMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the constant (translucency) texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveConstantMap(String format, File outputDirectory)
    {
        saveConstantMap(format, outputDirectory, null);
    }

    /**
     * Saves the ambient occlusion texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveOcclusionMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the ambient occlusion texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveOcclusionMap(String format, File outputDirectory)
    {
        saveOcclusionMap(format, outputDirectory, null);
    }

    /**
     * Saves the albedo texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveAlbedoMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the albedo texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveAlbedoMap(String format, File outputDirectory)
    {
        saveAlbedoMap(format, outputDirectory, null);
    }

    /**
     * Saves the ORM (occlusion/roughness/metallicity) texture to the filesystem in the specified format.
     *
     * @param format           The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory  The directory in which to save the texture.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveORMMap(String format, File outputDirectory, String filenameOverride);

    /**
     * Saves the ORM (occlusion/roughness/metallicity) texture to the filesystem in the specified format, with a default filename.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the texture.
     */
    default void saveORMMap(String format, File outputDirectory)
    {
        saveORMMap(format, outputDirectory, null);
    }

    /**
     * Saves weight map textures to the filesystem in the specified format,
     * with four weight maps packed into a single image in the RGBA channels.
     *
     * @param format            The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory   The directory in which to save the textures.
     * @param filenameOverrides A function that provides the filename as a function of the weight map index.
     *                          If set to null, default filenames will be provided.
     */
    void savePackedWeightMaps(String format, File outputDirectory, IntFunction<String> filenameOverrides);

    /**
     * Saves packed weight map textures to the filesystem in the specified format
     * with four weight maps packed into a single image in the RGBA channels, using default filenames.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     */
    default void savePackedWeightMaps(String format, File outputDirectory)
    {
        savePackedWeightMaps(format, outputDirectory, null);
    }

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images.
     *
     * @param format            The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory   The directory in which to save the textures.
     * @param filenameOverrides A function that provides the filename as a function of the weight map index.
     *                          If set to null, default filenames will be provided.
     */
    void saveUnpackedWeightMaps(String format, File outputDirectory, IntFunction<String> filenameOverrides);

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images, using default filenames.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     */
    default void saveUnpackedWeightMaps(String format, File outputDirectory)
    {
        saveUnpackedWeightMaps(format, outputDirectory, null);
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


    /**
     * Saves all metadata maps to the filesystem as images in the specified format.
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the metadata maps.
     * @param filenamePrefix  A prefix to attach to each file (i.e. the name of the project).
     *                        This can be set to the empty string "" to use just the base / default names.
     */
    void saveMetadataMaps(String format, File outputDirectory, String filenamePrefix);

    /**
     * Saves all resources to the specified output directory with the specified image format, using default filenames.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory
     */
    default void saveAll(String format, File outputDirectory)
    {
        saveEssential(format, outputDirectory);
        saveOcclusionMap(format, outputDirectory, null);
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

    /**
     * Saves essential resources to the specified output directory using default filenames.
     * Skips standalone occlusion map and unpacked weight maps.
     * Includes standalone roughness map for more convenient Blender / Sketchfab access.
     *
     * @param outputDirectory
     */
    default void saveEssential(String format, File outputDirectory)
    {
        saveDiffuseMap(format, outputDirectory, null);
        saveNormalMap(format, outputDirectory, null);
        saveConstantMap(format, outputDirectory, null);
        saveAlbedoMap(format, outputDirectory, null);
        saveORMMap(format, outputDirectory, null);
        saveSpecularReflectivityMap(format, outputDirectory, null);
        saveSpecularRoughnessMap(format, outputDirectory, null);
        savePackedWeightMaps(format, outputDirectory, null);
        saveBasisFunctions(outputDirectory, null);
        saveMetadataMaps(format, outputDirectory, "");
    }


    /**
     * Deletes one of the basis materials.
     * This will cause the basis materials, weight maps, and thumbnail images to be automatically re-saved
     * to the project's supporting files directory.
      * @param materialIndex
     */
    void deleteBasisMaterial(int materialIndex);
}
