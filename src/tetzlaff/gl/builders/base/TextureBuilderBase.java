/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.builders.base;

import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture;

public abstract class TextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> implements TextureBuilder<ContextType, TextureType>
{
    protected final ContextType context;
    private int multisamples = 1;
    private boolean fixedMultisampleLocations = true;
    private boolean mipmapsEnabled = false;
    private boolean linearFilteringEnabled = false;
    private float maxAnisotropy = 1.0f;

    protected int getMultisamples()
    {
        return multisamples;
    }

    protected boolean areMultisampleLocationsFixed()
    {
        return fixedMultisampleLocations;
    }

    protected boolean areMipmapsEnabled()
    {
        return mipmapsEnabled;
    }

    protected boolean isLinearFilteringEnabled()
    {
        return linearFilteringEnabled;
    }

    protected float getMaxAnisotropy()
    {
        return maxAnisotropy;
    }

    protected TextureBuilderBase(ContextType context)
    {
        this.context = context;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        multisamples = samples;
        fixedMultisampleLocations = fixedSampleLocations;
        return this;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        mipmapsEnabled = enabled;
        return this;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        linearFilteringEnabled = enabled;
        return this;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        this.maxAnisotropy = maxAnisotropy;
        return this;
    }
}
