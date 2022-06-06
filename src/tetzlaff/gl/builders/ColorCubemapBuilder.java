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

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

public interface ColorCubemapBuilder <ContextType extends Context<ContextType>, TextureType extends Cubemap<ContextType>>
extends ColorTextureBuilder<ContextType, TextureType>
{
    ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, NativeVectorBuffer data);

    @Override
    ColorCubemapBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
    @Override
    ColorCubemapBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);

    @Override
    ColorCubemapBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    ColorCubemapBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    ColorCubemapBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    ColorCubemapBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
