/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.builders.base;

import tetzlaff.gl.builders.ColorCubemapBuilder;
import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.CompressionFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Cubemap;

public abstract class ColorCubemapBuilderBase <ContextType extends Context<ContextType>, TextureType extends Cubemap<ContextType>> 
    extends ColorTextureBuilderBase<ContextType, TextureType>
    implements ColorCubemapBuilder<ContextType, TextureType>
{
    protected ColorCubemapBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setInternalFormat(ColorFormat format)
    {
        super.setInternalFormat(format);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setInternalFormat(CompressionFormat format)
    {
        super.setInternalFormat(format);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMaxMipmapLevel(int maxMipmapLevel)
    {
        super.setMaxMipmapLevel(maxMipmapLevel);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
