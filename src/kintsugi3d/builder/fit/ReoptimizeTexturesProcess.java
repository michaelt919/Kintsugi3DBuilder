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

package kintsugi3d.builder.fit;

import kintsugi3d.builder.fit.decomposition.MaterialBasis;
import kintsugi3d.builder.fit.settings.ReadonlySpecularFitSettings;
import kintsugi3d.builder.resources.project.GraphicsResourcesCacheable;
import kintsugi3d.builder.resources.project.ImageCache;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResources;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.gl.interactive.UserCancellationException;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class ReoptimizeTexturesProcess extends SpecularFitProcess
{
    public ReoptimizeTexturesProcess(ReadonlySpecularFitSettings settings, File outputDirectory)
    {
        super(settings, outputDirectory);
    }

    public <ContextType extends Context<ContextType>> void reoptimizeTexturesWithCache(
        GraphicsResourcesCacheable<ContextType> resources, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        // Get cache (should already be generated).
        ImageCache<ContextType> cache = resources.cache(getSettings().getImageCacheSettings(), null);

        // Runs the fit (long process) and then replaces the old material resources / textures
        resources.replaceTextureResources(reoptimizeTexturesWithCache(cache, resources.getTextureResources(), monitor));
    }

    private <ContextType extends Context<ContextType>> TextureResources<ContextType> reoptimizeTexturesWithCache(
        ImageCache<ContextType> cache, ReadonlyTextureResources<ContextType> original, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        Instant start = Instant.now();

        ContextType context = cache.getContext();

        // Disable back face culling since we're rendering in texture space
        // (should be the case already from generating the cache, but good to do just in case)
        context.getState().disableBackFaceCulling();

        if (monitor != null)
        {
            monitor.setStageCount(1);
            monitor.setStage(0, "Performing high-res fit...");
        }

        MaterialBasis basis = original.getBasisResources().getBasis().copy();
        return optimizeFullResTexturesWithCache(cache, monitor, original, basis, start);
    }

    @Override
    protected <ContextType extends Context<ContextType>> SpecularFitResourcesWrapper<ContextType> getResourcesWrapper()
    {
        return new SpecularFitResourcesWrapper<>(getSettings().isSmithMaskingShadowingEnabled());
    }
}
