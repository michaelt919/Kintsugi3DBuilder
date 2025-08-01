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

package kintsugi3d.builder.resources.ibr;

import kintsugi3d.builder.core.DefaultProgressMonitor;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Resource;
import kintsugi3d.gl.geometry.GeometryTextures;
import kintsugi3d.gl.material.TextureLoadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TextureBlockResourceFactory<ContextType extends Context<ContextType>> implements Resource
{
    private static final Logger log = LoggerFactory.getLogger(TextureBlockResourceFactory.class);
    private final ImageCache<ContextType> imageCache;

    private final IBRSharedResources<ContextType> sharedResources;

    private final GeometryTextures<ContextType> fullGeometryTextures;

    TextureBlockResourceFactory(IBRSharedResources<ContextType> sharedResources, ImageCache<ContextType> imageCache)
    {
        this.imageCache = imageCache;
        this.sharedResources = sharedResources;

        this.fullGeometryTextures = sharedResources.getGeometryResources()
            .createGeometryFramebuffer(imageCache.getSettings().getTextureWidth(), imageCache.getSettings().getTextureHeight());
    }

    public IBRResourcesTextureSpace<ContextType> createBlockResources(int i, int j, ProgressMonitor monitor) throws IOException, UserCancellationException
    {
        TextureLoadOptions loadOptions = new TextureLoadOptions();
        loadOptions.setLinearFilteringRequested(false);
        loadOptions.setMipmapsRequested(false);
        loadOptions.setCompressionRequested(false);

        int x = imageCache.getSettings().getBlockStartX(i);
        int y = imageCache.getSettings().getBlockStartY(j);
        int width = imageCache.getSettings().getBlockStartX(i + 1) - x;
        int height = imageCache.getSettings().getBlockStartY(j + 1) - y;

        try
        {
            return new IBRResourcesTextureSpace<>(sharedResources,
                () -> fullGeometryTextures.createViewportCopy(x, y, width, height), imageCache.getSettings().getBlockDir(i, j),
                loadOptions, width, height, monitor);
        }
        catch (IOException e)
        {
            log.warn("Incomplete cache; will try to rebuild.");

            // Try to reinitialize in case the cache was only partially complete.
            imageCache.initialize(new DefaultProgressMonitor()
            {
                @Override
                public void allowUserCancellation() throws UserCancellationException
                {
                    monitor.allowUserCancellation();
                }
            });

            // If initialize() completed without exceptions, then createSampledResources() should work now.
            return createBlockResources(i, j, monitor);
        }
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
