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

package kintsugi3d.gl.builders;

import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.CompressionFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture;

public interface ColorTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
    ColorTextureBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
    ColorTextureBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);

    /**
     * Whether or not to use (transform to sRGB) or ignore (reinterpret as sRGB) any ICC transformation
     * specified in images loaded from file or input stream
     * @param iccTransformationRequested true if any ICC profile in the file should be used, false if it should be ignored.
     * @return The builder for chained method calls
     */
    ColorTextureBuilder<ContextType, TextureType> setICCTransformationRequested(boolean iccTransformationRequested);

    @Override
    ColorTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
