/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.builders;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.CompressionFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture;

public interface ColorTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
    ColorTextureBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
    ColorTextureBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);

    @Override
    ColorTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
