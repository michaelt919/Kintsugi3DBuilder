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

package kintsugi3d.builder.resources.project.texture;

import kintsugi3d.builder.core.texture.TextureInfo;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.fit.decomposition.MutableBasisResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CroppedTextureResources<ContextType extends Context<ContextType>>
    extends TextureResourcesBase<ContextType>
{
    private final Map<TextureInfo, Texture2D<ContextType>> textures;
    private final MutableBasisResources<ContextType> basisResources;

    private final ContextType context;
    private final int width;
    private final int height;

    CroppedTextureResources(
        ReadonlyTextureResources<ContextType> original, int cropX, int cropY, int cropWidth, int cropHeight)
    {
        this.width = cropWidth;
        this.height = cropHeight;
        this.context = original.getContext();

        textures = new HashMap<>(original.getTextures().size());
        for (var texEntry : original.getTextures().entrySet())
        {
            textures.put(texEntry.getKey(), texEntry.getValue().crop(cropX, cropY, cropWidth, cropHeight));
        }

        basisResources = new MutableBasisResources<>(context, original.getBasisResources().getBasis().copy(),
            () -> original.getBasisWeightResources().crop(cropX, cropY, cropWidth, cropHeight));
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }

    @Override
    public Map<TextureInfo, Texture2D<ContextType>> getTextures()
    {
        return Collections.unmodifiableMap(textures);
    }

    @Override
    protected MutableBasisResources<ContextType> getMutableBasisResources()
    {
        return basisResources;
    }


    @Override
    protected BasisWeightResources<ContextType> getMutableBasisWeightResources()
    {
        return basisResources.getWeightResources();
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public void close()
    {
        for (Texture2D<ContextType> texture : textures.values())
        {
            texture.close();
        }
        textures.clear();

        basisResources.close();
    }
}
