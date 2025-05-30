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

package kintsugi3d.gl.builders.base;

import kintsugi3d.gl.builders.DepthStencilTextureBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture;

public abstract class DepthStencilTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
    extends TextureBuilderBase<ContextType, TextureType> implements DepthStencilTextureBuilder<ContextType, TextureType>
{
    private boolean floatingPoint;

    protected boolean isFloatingPointEnabled()
    {
        return this.floatingPoint;
    }

    protected DepthStencilTextureBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public DepthStencilTextureBuilderBase<ContextType, TextureType> setFloatingPointDepthEnabled(boolean enabled)
    {
        this.floatingPoint = enabled;
        return this;
    }

    @Override
    public DepthStencilTextureBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public DepthStencilTextureBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public DepthStencilTextureBuilderBase<ContextType, TextureType> setMaxMipmapLevel(int maxMipmapLevel)
    {
        super.setMaxMipmapLevel(maxMipmapLevel);
        return this;
    }

    @Override
    public DepthStencilTextureBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public DepthStencilTextureBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
