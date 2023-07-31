/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.builders.base;

import kintsugi3d.gl.builders.StencilTextureBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture;

public abstract class StencilTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
    extends TextureBuilderBase<ContextType, TextureType> implements StencilTextureBuilder<ContextType, TextureType>
{
    private int precision = 8;

    protected int getInternalPrecision()
    {
        return this.precision;
    }

    protected StencilTextureBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
    {
        this.precision = precision;
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMaxMipmapLevel(int maxMipmapLevel)
    {
        super.setMaxMipmapLevel(maxMipmapLevel);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
