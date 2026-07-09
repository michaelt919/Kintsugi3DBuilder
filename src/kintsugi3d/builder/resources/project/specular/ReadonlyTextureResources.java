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
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.ContextBound;
import kintsugi3d.gl.core.Program;

import java.io.File;

public interface ReadonlyTextureResources<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
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

    void saveTexture(StandardTexture tex, String format, File outputDirectory, String filenameOverride);

    /**
     * Saves weight map textures to the filesystem in the specified format,
     * with four weight maps packed into a single image in the RGBA channels.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A string to be prepended to each weightmap's filename.
     */
    void savePackedWeightMaps(String format, File outputDirectory, String filenamePrefix);

    void savePackedWeightMaps(String format, File outputDirectory);

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A string to be prepended to each weightmap's filename.
     */
    void saveUnpackedWeightMaps(String format, File outputDirectory, String filenamePrefix);

    void saveUnpackedWeightMaps(String format, File outputDirectory);

    /**
     * Saves the basis function to the filesystem as a CSV file.
     *
     * @param outputDirectory  The directory in which to save the basis functions.
     * @param filenameOverride The filename to use.  If set to null, a default filename will be provided.
     */
    void saveBasisFunctions(File outputDirectory, String filenameOverride);

    void saveBasisFunctions(File outputDirectory);

    void saveNamedTextures(Iterable<String> texNames, String format, File outputDirectory, String filenamePrefix);

    void saveAllNamedTextures(String format, File outputDirectory, String filenamePrefix);

    void saveAll(String format, File outputDirectory);

    void saveAll(File outputDirectory);
}
