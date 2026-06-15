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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.core.TextureDetails;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleTextureResources<ContextType extends Context<ContextType>>
    extends TextureResourcesBase<ContextType>
{
    private final Map<TextureDetails, Texture2D<ContextType>> textures = new HashMap<>(StandardTexture.values().length);
    private BasisResources<ContextType> basisResources;
    private BasisWeightResources<ContextType> basisWeightResources;

    private final ContextType context;

    /**
     * Package-visible default constructor
     */
    SimpleTextureResources(ContextType context)
    {
        this.context = context;
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public int getWidth()
    {
        // TODO validation that textures all have the same resolution?
        return basisWeightResources.weightMaps.getWidth();
    }

    @Override
    public int getHeight()
    {
        // TODO validation that textures all have the same resolution?
        return basisWeightResources.weightMaps.getHeight();
    }

    @Override
    public Map<TextureDetails, Texture2D<ContextType>> getTextures()
    {
        return Collections.unmodifiableMap(textures);
    }

    public void setTexture(String texName, Texture2D<ContextType> texture)
    {
        textures.put(new TextureDetails(texName), texture);
    }

    public void setTexture(TextureDetails textureDetails, Texture2D<ContextType> texture)
    {
        textures.put(textureDetails, texture);
    }

    public void setTexture(StandardTexture standardTex, Texture2D<ContextType> texture)
    {
        textures.put(standardTex.details, texture);
    }

    @Override
    public BasisResources<ContextType> getBasisResources()
    {
        return basisResources;
    }

    @Override
    public BasisWeightResources<ContextType> getBasisWeightResources()
    {
        return basisWeightResources;
    }

    @Override
    public void close()
    {
        for (Texture2D<ContextType> tex : textures.values())
        {
            tex.close();
        }

        textures.clear();

        if (basisResources != null)
        {
            basisResources.close();
            basisResources = null;
        }

        if (basisWeightResources != null)
        {
            basisWeightResources.close();
            basisWeightResources = null;
        }
    }
}
