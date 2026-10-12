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

package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.texture.TextureInfo;
import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.fit.decomposition.MutableBasisResources;
import kintsugi3d.builder.resources.project.texture.ReadonlyTextureResources;
import kintsugi3d.builder.resources.project.texture.TextureResourcesBase;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class SampledTextureResources<ContextType extends Context<ContextType>> extends TextureResourcesBase<ContextType>
{
    private final TextureResolution resolution;
    private final ContextType context;

    private final MutableBasisResources<ContextType> basisResources;
    private final Map<TextureInfo, Texture2D<ContextType>> textures;

    SampledTextureResources(ReadonlyTextureResources<ContextType> original, TextureResolution resolution,
                            BiConsumer<Consumer<ReadonlyNativeVectorBuffer>, ColorTextureReader> sampleFunction)
    {
        this.resolution = resolution;
        this.context = original.getContext();

        textures = new HashMap<>(original.getTextures().size());
        for (var texEntry : original.getTextures().entrySet())
        {
            // Create a matching texture in the target resolution and then use the sample function to populate it.
            ReadonlyTexture2D<ContextType> originalTexture = texEntry.getValue();
            Texture2D<ContextType> sampledTexture =
                originalTexture.createTextureWithMatchingFormat(resolution.width, resolution.height);
            sampleFunction.accept(sampledTexture::load, originalTexture.getColorTextureReader());
            textures.put(texEntry.getKey(), sampledTexture);
        }

        // Create basis resources with weightmaps in the target resolution
        basisResources = new MutableBasisResources<>(context, original.getBasisResources().getBasis().copy(), resolution);
        Texture3D<ContextType> weightMaps = basisResources.getWeightResources().getWeightMaps();

        ReadonlyTexture3D<ContextType> originalWeightMaps = original.getBasisWeightResources().getWeightMaps();
        for (int b = 0; b < weightMaps.getDepth(); b++)
        {
            // use the sample function to populate each layer of the weight maps array.
            int layer = b;
            sampleFunction.accept(data -> weightMaps.loadLayer(layer, data),
                originalWeightMaps.getColorTextureReader(layer));
        }
    }

    @Override
    public int getWidth()
    {
        return resolution.width;
    }

    @Override
    public int getHeight()
    {
        return resolution.height;
    }

    @Override
    public ContextType getContext()
    {
        return context;
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
    public Map<TextureInfo, ? extends Texture2D<ContextType>> getTextures()
    {
        return Collections.unmodifiableMap(textures);
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
