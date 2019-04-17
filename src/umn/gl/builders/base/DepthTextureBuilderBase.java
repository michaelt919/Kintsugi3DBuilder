/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.builders.base;

import umn.gl.builders.DepthTextureBuilder;
import umn.gl.core.Context;
import umn.gl.core.Texture;

public abstract class DepthTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
    extends TextureBuilderBase<ContextType, TextureType> implements DepthTextureBuilder<ContextType, TextureType>
{
    private int precision = 8;
    private boolean floatingPoint;

    protected int getInternalPrecision()
    {
        return this.precision;
    }

    protected boolean isFloatingPointEnabled()
    {
        return this.floatingPoint;
    }

    protected DepthTextureBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
    {
        this.precision = precision;
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setFloatingPointEnabled(boolean enabled)
    {
        this.floatingPoint = enabled;
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMaxMipmapLevel(int maxMipmapLevel)
    {
        super.setMaxMipmapLevel(maxMipmapLevel);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
