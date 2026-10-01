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

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.gl.core.*;

import java.io.File;
import java.util.function.IntFunction;

public interface ReadonlyBasisWeightResources<ContextType extends Context<ContextType>> extends ContextBound<ContextType>, Croppable<BasisWeightResources<ContextType>>
{
    int getMaterialCount();
    int getActiveMaterialCount();

    ReadonlyTexture3D<ContextType> getWeightMaps();
    ReadonlyTexture2D<ContextType> getWeightMask();

    void useWithShaderProgram(Program<ContextType> program);

    /**
     * Saves packed weight map textures to the filesystem in the specified format.
     * Only saves weight maps for basis materials that are enabled.
     *
     * @param weightsPerImage How many weightmaps to pack per image.
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenameGenerator Function that produces the filename for each numbered weightmap.
     */
    void savePacked(int weightsPerImage, String format, File outputDirectory, IntFunction<String> filenameGenerator);

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images.
     * Only saves weight maps for basis materials that are enabled.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     * @param filenamePrefix  A string to be prepended to each weightmap's filename.
     */
    void saveUnpacked(String format, File outputDirectory, String filenamePrefix);

    /**
     * Saves unpacked weight map textures to the filesystem in the specified format as grayscale images, using default filenames.
     * Only saves weight maps for basis materials that are enabled.
     *
     * @param format          The image format to use.  PNG, JPEG, and TIFF are supported.
     * @param outputDirectory The directory in which to save the textures.
     */
    default void saveUnpacked(String format, File outputDirectory)
    {
        saveUnpacked(format, outputDirectory, "");
    }
}
