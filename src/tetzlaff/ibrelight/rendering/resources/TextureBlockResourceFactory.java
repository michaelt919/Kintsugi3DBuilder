/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Resource;
import tetzlaff.gl.geometry.GeometryTextures;
import tetzlaff.gl.material.TextureLoadOptions;

import java.io.IOException;

public class TextureBlockResourceFactory<ContextType extends Context<ContextType>> implements Resource
{
    private final ImageCache<ContextType> imageCache;

    private final IBRSharedResources<ContextType> sharedResources;

    private final GeometryTextures<ContextType> fullGeometryTextures;

    TextureBlockResourceFactory(ImageCache<ContextType> imageCache, IBRSharedResources<ContextType> sharedResources)
    {
        this.imageCache = imageCache;
        this.sharedResources = sharedResources;

        ImageCacheSettings settings = imageCache.getSettings();
        this.fullGeometryTextures = sharedResources.getGeometryResources()
            .createGeometryFramebuffer(settings.getTextureWidth(), settings.getTextureHeight());
    }

    public IBRResources<ContextType> createBlockResources(int i, int j) throws IOException
    {
        TextureLoadOptions loadOptions = new TextureLoadOptions();
        loadOptions.setLinearFilteringRequested(false);
        loadOptions.setMipmapsRequested(false);
        loadOptions.setCompressionRequested(false);

        int x = imageCache.getBlockStartX(i);
        int y = imageCache.getBlockStartY(j);
        int width = imageCache.getBlockStartX(i + 1) - x;
        int height = imageCache.getBlockStartY(j + 1) - y;

        return new IBRResourcesTextureSpace<>(sharedResources,
            () -> fullGeometryTextures.createViewportCopy(x, y, width, height), imageCache.getBlockDir(i, j),
            loadOptions, width, height, null);
    }

    @Override
    public void close()
    {
        if (this.fullGeometryTextures != null)
        {
            fullGeometryTextures.close();
        }
    }
}
