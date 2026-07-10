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
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisResources;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisWeightResources;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public interface ReadonlyTextureResources<ContextType extends Context<ContextType>>
    extends ContextBound<ContextType>, TwoDimensional
{
    Map<TextureDetails, ? extends ReadonlyTexture2D<ContextType>> getTextures();

    /**
     * Returns a map containing only the standard textures
     * @return
     */
    default Map<StandardTexture, ? extends ReadonlyTexture2D<ContextType>> getStandardTextures()
    {
        return StandardTexture.convertObjectMapToEnumMap(getTextures());
    }

    default ReadonlyTexture2D<ContextType> getTexture(String texName)
    {
        return getTextures().get(new TextureDetails(texName));
    }

    default ReadonlyTexture2D<ContextType> getTexture(TextureDetails tex)
    {
        return getTextures().get(tex);
    }

    default ReadonlyTexture2D<ContextType> getTexture(StandardTexture tex)
    {
        return getTextures().get(tex.details);
    }

    ReadonlyBasisResources<ContextType> getBasisResources();

    ReadonlyBasisWeightResources<ContextType> getBasisWeightResources();

    void setupShaderProgram(Program<ContextType> program);

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
    default void saveTexture(StandardTexture tex, String format, File outputDirectory, String filenameOverride)
    {
        saveTexture(tex.details.name, format, outputDirectory, filenameOverride);
    }

    /**
     * Saves weight map textures to the filesystem in the specified format,
     * with four weight maps packed into a single image in the RGBA channels.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A string to be prepended to each weightmap's filename.
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
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A string to be prepended to each weightmap's filename.
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

    /**
     * Saves the specified named textures to the filesystem as images in the specified format.
     * @param texNames        The names of the textures to save.
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A prefix to attach to each file (i.e. the name of the project).
     *                        This can be set to the empty string "" to use just the base / default names.
     */
    default void saveNamedTextures(Iterable<String> texNames, String format, File outputDirectory, String filenamePrefix)
    {
        for (String name : texNames)
        {
            saveTexture(name, format, outputDirectory, TextureResources.getTextureFilename(name, format, filenamePrefix));
        }
    }

    /**
     * Saves all named textures (but not weight maps) to the filesystem as images in the specified format.
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A prefix to attach to each file (i.e. the name of the project).
     *                        This can be set to the empty string "" to use just the base / default names.
     */
    default void saveAllNamedTextures(String format, File outputDirectory, String filenamePrefix)
    {
        saveNamedTextures(getTextures().keySet().stream().map(t -> t.name).collect(Collectors.toList()),
            format, outputDirectory, filenamePrefix);
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
        savePackedWeightMaps(format, outputDirectory, "");
        saveUnpackedWeightMaps(format, outputDirectory, "");
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
}
