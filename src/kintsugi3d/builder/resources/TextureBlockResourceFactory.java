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

package kintsugi3d.builder.resources;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Resource;
import kintsugi3d.gl.geometry.GeometryTextures;
import kintsugi3d.gl.material.TextureLoadOptions;

import java.io.IOException;

public class TextureBlockResourceFactory<ContextType extends Context<ContextType>> implements Resource
{
    private final ImageCacheSettings imageCacheSettings;

    private final IBRSharedResources<ContextType> sharedResources;

    private final GeometryTextures<ContextType> fullGeometryTextures;

    TextureBlockResourceFactory(IBRSharedResources<ContextType> sharedResources, ImageCacheSettings imageCacheSettings)
    {
        this.imageCacheSettings = imageCacheSettings;
        this.sharedResources = sharedResources;

        this.fullGeometryTextures = sharedResources.getGeometryResources()
            .createGeometryFramebuffer(imageCacheSettings.getTextureWidth(), imageCacheSettings.getTextureHeight());
    }

    public IBRResourcesTextureSpace<ContextType> createBlockResources(int i, int j) throws IOException
    {
        TextureLoadOptions loadOptions = new TextureLoadOptions();
        loadOptions.setLinearFilteringRequested(false);
        loadOptions.setMipmapsRequested(false);
        loadOptions.setCompressionRequested(false);

        int x = imageCacheSettings.getBlockStartX(i);
        int y = imageCacheSettings.getBlockStartY(j);
        int width = imageCacheSettings.getBlockStartX(i + 1) - x;
        int height = imageCacheSettings.getBlockStartY(j + 1) - y;

        return new IBRResourcesTextureSpace<>(sharedResources,
            () -> fullGeometryTextures.createViewportCopy(x, y, width, height), imageCacheSettings.getBlockDir(i, j),
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
