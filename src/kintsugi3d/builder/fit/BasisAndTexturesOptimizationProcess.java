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

import kintsugi3d.builder.fit.decomposition.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromScratch;
import kintsugi3d.builder.fit.settings.ReadonlyBasisOptimizationSettings;
import kintsugi3d.builder.fit.settings.ReadonlySpecularFitSettings;
import kintsugi3d.builder.resources.project.GraphicsResourcesTextureSpace;
import kintsugi3d.builder.resources.project.ImageCache;
import kintsugi3d.builder.resources.project.ReadonlyImageBasedGraphicsResources;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.interactive.DefaultProgressMonitor;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.gl.interactive.UserCancellationException;
import kintsugi3d.optimization.function.BasisFunctions;
import kintsugi3d.optimization.function.GeneralizedSmoothStepBasis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;

public class BasisAndTexturesOptimizationProcess extends SpecularFitProcess
{
    private static final Logger LOG = LoggerFactory.getLogger(BasisAndTexturesOptimizationProcess.class);

    private final ReadonlyBasisOptimizationSettings basisOptimizationSettings;

    public BasisAndTexturesOptimizationProcess(ReadonlySpecularFitSettings settings,
                                               ReadonlyBasisOptimizationSettings basisOptimizationSettings,
                                               File outputDirectory)
    {
        super(settings, outputDirectory);
        this.basisOptimizationSettings = basisOptimizationSettings;
    }

    public <ContextType extends Context<ContextType>> TextureResources<ContextType> optimizeFitWithCache(
        ReadonlyImageBasedGraphicsResources<ContextType> resources, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        Instant start = Instant.now();

        if (monitor != null)
        {
            monitor.setStageCount(3);
            monitor.setStage(0, "Building cache...");
        }

        // Generate cache
        ImageCache<ContextType> cache = resources.cache(getSettings().getImageCacheSettings(), monitor);

        Duration duration = Duration.between(start, Instant.now());
        LOG.info("Cache found / generated in: {}", duration);

        // Runs the fit (long process) and then returns the old material resources / textures
        return optimizeFitWithCache(cache, monitor);
    }

    private <ContextType extends Context<ContextType>> TextureResources<ContextType> optimizeFitWithCache(
        ImageCache<ContextType> cache, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        Instant start = Instant.now();

        if (monitor != null)
        {
            monitor.setStage(1, "Performing low-res fit...");
        }

        try (GraphicsResourcesTextureSpace<ContextType> sampled = cache.createSampledResources(
            new DefaultProgressMonitor() // simple progress monitor for logging; will not be shown in the UI
            {
                private double maxProgress = 0.0;

                @Override
                public void allowUserCancellation() throws UserCancellationException
                {
                    if (monitor != null)
                    {
                        monitor.allowUserCancellation();
                    }
                }

                @Override
                public void setMaxProgress(double maxProgress)
                {
                    this.maxProgress = maxProgress;
                }

                @Override
                public void setProgress(double progress, String message)
                {
                    LOG.info("[{}%] {}", new DecimalFormat("#.##").format(progress / maxProgress * 100), message);
                }
            }))
        {
            ContextType context = sampled.getContext();
            // Disable back face culling since we're rendering in texture space
            // (should be the case already from generating the cache, but good to do just in case)
            context.getState().disableBackFaceCulling();

            SpecularDecompositionFromScratch sampledDecomposition =
                new SpecularDecompositionFromScratch(sampled.getTextureResolution(), basisOptimizationSettings);

            try (SpecularFitOptimizable<ContextType> sampledFit = optimizeFit(sampled, sampledDecomposition, monitor)) // low-res fit happens here; takes a while
            {
                if (monitor != null)
                {
                    monitor.setStage(2, "Performing high-res fit...");
                }

                return optimizeFullResTexturesWithCache(cache, monitor, sampledFit, sampledDecomposition.getMaterialBasis(), start);
            }
        }
    }

    public <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> optimizeFit(
        ReadonlyImageBasedGraphicsResources<ContextType> resources, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        SpecularDecompositionFromScratch decomposition =
            new SpecularDecompositionFromScratch(getSettings().getTextureResolution(), basisOptimizationSettings);
        return optimizeFit(resources, decomposition, monitor);
    }

    private <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> optimizeFit(
        ReadonlyImageBasedGraphicsResources<ContextType> resources, SpecularDecompositionFromScratch decomposition, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        SpecularFitResourcesWrapper<ContextType> programFactory = getResourcesWrapper();

        // Initialize weights using K-means.
        SpecularFitInitializer<ContextType> initializer =
            new SpecularFitInitializer<>(programFactory.wrap(resources), basisOptimizationSettings);
        initializer.initialize(decomposition);

        if (DEBUG_IMAGES && getOutputDirectory() != null)
        {
            initializer.saveDebugImage(decomposition, getOutputDirectory());
        }

        SpecularFitOptimizable<ContextType> specularFit = SpecularFitOptimizable.createNew(
            resources, programFactory, decomposition.getTextureResolution(), basisOptimizationSettings,
            getSettings().getNormalOptimizationSettings(), false);

        try
        {
            BasisFunctions basisFunctions = new GeneralizedSmoothStepBasis(
                basisOptimizationSettings.getBasisResolution(),
                basisOptimizationSettings.getMetallicity(),
                basisOptimizationSettings.getSpecularMinWidth(),
                basisOptimizationSettings.getSpecularMaxWidth(),
                basisOptimizationSettings.getBasisComplexity(),
                x -> 3 * x * x - 2 * x * x * x);
//                new StepBasis(settings.microfacetDistributionResolution, settings.getMetallicity())

                // Preliminary optimization at low resolution to determine basis functions
            this.optimizeTexSpaceFit(resources, decomposition.getTextureResolution(),
                (stream, monitorLocal) -> specularFit.optimizeFromScratch(
                    basisOptimizationSettings, decomposition, stream, getSettings().getPreliminaryConvergenceTolerance(),
                    monitorLocal, TRACE_IMAGES && getOutputDirectory() != null ? getOutputDirectory() : null),
                monitor);

//            if (getOutputDirectory() != null)
//            {
//                // Save the final basis functions
//                decomposition.saveBasisFunctions(getOutputDirectory());
//            }

            if (DEBUG_IMAGES && getOutputDirectory() != null)
            {
                // write out diffuse texture for debugging
                decomposition.saveDiffuseMap(getOutputDirectory());

                // Save basis image visualization for reference and debugging
                try (BasisImageCreator<ContextType> basisImageCreator =
                        new BasisImageCreator<>(resources.getContext(), basisOptimizationSettings.getBasisResolution()))
                {
                    basisImageCreator.createImages(specularFit, getOutputDirectory());
                }
            }

            return specularFit;
        }
        catch (RuntimeException | UserCancellationException e)
        {
            specularFit.close();
            throw e;
        }
    }

    @Override
    protected <ContextType extends Context<ContextType>> SpecularFitResourcesWrapper<ContextType> getResourcesWrapper()
    {
        return new SpecularFitResourcesWrapper<>(getSettings().isSmithMaskingShadowingEnabled(), basisOptimizationSettings);
    }
}
