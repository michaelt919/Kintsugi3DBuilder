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

package kintsugi3d.builder.fit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;

import kintsugi3d.builder.core.DefaultProgressMonitor;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.export.specular.SpecularFitTextureRescaler;
import kintsugi3d.builder.fit.debug.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.SpecularDecomposition;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromExistingBasis;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromScratch;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.resources.ibr.*;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamResource;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Blittable;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.gl.material.ReadonlyMaterial;
import kintsugi3d.gl.material.ReadonlyMaterialTextureMap;
import kintsugi3d.util.ImageFinder;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularFitProcess
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitProcess.class);
    private static final boolean DEBUG_IMAGES = false;
    private static final boolean TRACE_IMAGES = false;

    private final SpecularFitRequestParams settings;

    public SpecularFitProcess(SpecularFitRequestParams settings)
    {
        this.settings = settings;
    }

    private <ContextType extends Context<ContextType>> SpecularFitProgramFactory<ContextType> getProgramFactory()
    {
        return new SpecularFitProgramFactory<>(
            settings.getIbrSettings(), settings.getSpecularBasisSettings());
    }

    public <ContextType extends Context<ContextType>> void optimizeFit(
        IBRResourcesImageSpace<ContextType> resources, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        Instant start = Instant.now();

        if (monitor != null)
        {
            monitor.setStageCount(3);
            monitor.setStage(0, "Building cache...");
        }

        // Generate cache
        ImageCache<ContextType> cache = resources.cache(settings.getImageCacheSettings(), monitor);

        Duration duration = Duration.between(start, Instant.now());
        log.info("Cache found / generated in: " + duration);

        // Runs the fit (long process) and then replaces the old material resources / textures
        resources.replaceSpecularMaterialResources(optimizeFit(cache, resources.getSpecularMaterialResources(), monitor));

//        // Save basis image visualization for reference and debugging
//        try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(cache.getContext(), settings.getSpecularBasisSettings()))
//        {
//            basisImageCreator.createImages(specularFit, settings.getOutputDirectory());
//        }

        // TODO: Final error calculation causes TDR for high-res textures
        // and is also not accurate as it doesn't load the full resolution images
        // Need to completely rework error metrics
//        // Calculate reasonable image resolution for error calculation
//        int imageWidth = determineImageWidth(resources.getViewSet());
//        int imageHeight = determineImageHeight(resources.getViewSet());
//
//        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();
//
//        try (ShaderBasedErrorCalculator<ContextType> errorCalculator =
//             ShaderBasedErrorCalculator.create(
//                cache.getContext(),
//                () -> createErrorCalcProgram(resources, programFactory),
//                program -> createErrorCalcDrawable(specularFit, resources, program),
//                imageWidth, imageHeight);
//             PrintStream rmseOut = new PrintStream(new File(settings.getOutputDirectory(), "rmse.txt")))
//        {
//            FinalErrorCalculaton finalErrorCalculaton = FinalErrorCalculaton.getInstance();
//
//            // Validate normals using input normal map (mainly for testing / experiment validation, not typical applications)
//            finalErrorCalculaton.validateNormalMap(resources, specularFit, rmseOut);
//
//            // Fill holes in weight maps and calculate some final error statistics.
//            finalErrorCalculaton.calculateFinalErrorMetrics(resources, programFactory, specularFit, errorCalculator, rmseOut);
//        }


        rescaleTextures();
    }

    private <ContextType extends Context<ContextType>> SpecularMaterialResources<ContextType> optimizeFit(
        ImageCache<ContextType> cache, SpecularMaterialResources<ContextType> original, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        Instant start = Instant.now();

        if (monitor != null)
        {
            monitor.setStage(1, "Performing low-res fit...");
        }

        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        try (IBRResourcesTextureSpace<ContextType> sampled = cache.createSampledResources(
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
                        log.info("[{}%] {}", new DecimalFormat("#.##").format(progress / maxProgress * 100), message);
                    }
                }))
        {
            ContextType context = sampled.getContext();
            // Disable back face culling since we're rendering in texture space
            // (should be the case already from generating the cache, but good to do just in case)
            context.getState().disableBackFaceCulling();

            TextureResolution sampledSettings = sampled.getTextureResolution();
            SpecularDecompositionFromScratch sampledDecomposition = new SpecularDecompositionFromScratch(sampledSettings, settings.getSpecularBasisSettings());

            // Initialize weights using K-means.
            SpecularFitInitializer<ContextType> initializer = new SpecularFitInitializer<>(sampled, settings.getSpecularBasisSettings());
            initializer.initialize(programFactory, sampledDecomposition);

            if (DEBUG_IMAGES && settings.getOutputDirectory() != null)
            {
                initializer.saveDebugImage(sampledDecomposition, settings.getOutputDirectory());
            }

            try
            (
                SpecularFitOptimizable<ContextType> sampledFit = SpecularFitOptimizable.createNew(
                    sampled, programFactory, sampledSettings, settings.getGamma(), settings.getSpecularBasisSettings(),
                    settings.getNormalOptimizationSettings(), false)
            )
            {
                // Preliminary optimization at low resolution to determine basis functions
                optimizeTexSpaceFit(sampled, (stream, monitorLocal) -> sampledFit.optimizeFromScratch(
                    sampledDecomposition, stream, settings.getPreliminaryConvergenceTolerance(),
                    monitorLocal, TRACE_IMAGES && settings.getOutputDirectory() != null ? settings.getOutputDirectory() : null),
                    sampledFit, monitor
                );

//                if (settings.getOutputDirectory() != null)
//                {
//                    // Save the final basis functions
//                    sampledDecomposition.saveBasisFunctions(settings.getOutputDirectory());
//                }

                if (DEBUG_IMAGES && settings.getOutputDirectory() != null)
                {
                    // write out diffuse texture for debugging
                    sampledDecomposition.saveDiffuseMap(settings.getGamma(), settings.getOutputDirectory());

                    // Save basis image visualization for reference and debugging
                    try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(cache.getContext(), settings.getSpecularBasisSettings()))
                    {
                        basisImageCreator.createImages(sampledFit, settings.getOutputDirectory());
                    }
                }

                if (monitor != null)
                {
                    monitor.setStage(2, "Performing high-res fit...");
                }

                // Create space for the solution.
                // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
                SpecularFitFinal<ContextType> fullResolution = SpecularFitFinal.createEmpty(original,
                    settings.getTextureResolution(), sampledFit.getMetadataMaps().keySet(), /* include all metadata maps supported by SpecularFitOptimizable */
                    settings.getSpecularBasisSettings(), settings.shouldIncludeConstantTerm());

                try
                {
                    // Basis functions are not spatial, so we want to just copy for future use
                    // Copy from CPU since 1D texture arrays can't apparently be attached to an FBO (as necessary for blitting)
                    fullResolution.getBasisResources().updateFromSolution(sampledDecomposition);

                    File geometryFile = sampled.getViewSet().getGeometryFile();
                    ReadonlyMaterial material = sampled.getGeometry().getMaterial();
                    ReadonlyMaterialTextureMap normalMap = material == null ? null : material.getNormalMap();
                    File inputNormalMap = geometryFile == null || material == null || normalMap == null ? null :
                        ImageFinder.getInstance().tryFindImageFile(new File(geometryFile.getParentFile(), normalMap.getMapName()));

                    // Optimize weight maps and normal maps by blocks to fill the full resolution textures
                    optimizeBlocks(fullResolution, cache, sampledDecomposition, inputNormalMap, monitor);

                    if (monitor != null)
                    {
                        // Go back to indeterminate for wrap-up stuff
                        monitor.setStage(3, "Texture processing complete.");
                    }

                    // Generate albedo / ORM maps at full resolution (does not require loaded source images)
                    fullResolution.getAlbedoORMOptimization().execute(fullResolution, settings.getGamma());

                    Duration duration = Duration.between(start, Instant.now());
                    log.info("Total processing time: " + duration);

                    if (DEBUG_IMAGES && settings.getOutputDirectory() != null)
                    {
                        try (PrintStream time = new PrintStream(new File(settings.getOutputDirectory(), "time.txt"), StandardCharsets.UTF_8))
                        {
                            time.println(duration);
                        }
                        catch (FileNotFoundException e)
                        {
                            log.error("An error occurred writing time file:", e);
                        }
                    }

                    if (settings.getOutputDirectory() != null)
                    {
                        fullResolution.saveAll(settings.getOutputDirectory());
                    }

                    return fullResolution;
                }
                catch (IOException|RuntimeException e)
                {
                    // Prevent memory leak when an exception occurs
                    fullResolution.close();

                    throw e;
                }
            }
        }
    }

    private <ContextType extends Context<ContextType>> void optimizeBlocks(
        Blittable<SpecularMaterialResources<ContextType>> fullResolutionDestination,
        ImageCache<ContextType> cache,
        SpecularDecompositionFromScratch sampledDecomposition,
        File inputNormalMapFile,
        ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        try(TextureBlockResourceFactory<ContextType> blockResourceFactory = cache.createBlockResourceFactory();
            Texture2D<ContextType> initialNormalMap = cache.getContext().getTextureFactory()
                .build2DColorTexture(fullResolutionDestination.getWidth(), fullResolutionDestination.getHeight())
                .setInternalFormat(ColorFormat.RGBA8)
                .setMipmapsEnabled(false)
                .setLinearFilteringEnabled(true)
                .createTexture())
        {
            if (inputNormalMapFile != null)
            {
                // Reload input normal map since it needs to be uncompressed for blit operations
                try (Texture2D<ContextType> inputNormalMap = cache.getContext().getTextureFactory()
                    .build2DColorTextureFromFile(inputNormalMapFile, true)
                    .setInternalFormat(ColorFormat.RGBA8)
                    .setMipmapsEnabled(false)
                    .setLinearFilteringEnabled(true)
                    .createTexture())
                {
                    // May need to rescale to the target resolution
                    initialNormalMap.blitScaled(inputNormalMap, true);
                }
            }

            if (monitor != null)
            {
                monitor.setMaxProgress(cache.getSettings().getTextureSubdiv() * cache.getSettings().getTextureSubdiv());
            }

            // Optimize each block of the texture map at full resolution.
            for (int i = 0; i < cache.getSettings().getTextureSubdiv(); i++)
            {
                for (int j = 0; j < cache.getSettings().getTextureSubdiv(); j++)
                {
                    int blockProgress = i * cache.getSettings().getTextureSubdiv() + j;

                    if (monitor != null)
                    {
                        monitor.setProgress(blockProgress, MessageFormat.format("Block ({0}, {1})", i, j));
                        monitor.allowUserCancellation();
                    }

                    try (IBRResourcesTextureSpace<ContextType> blockResources = blockResourceFactory.createBlockResources(i, j,
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
                                    log.info("[{}%] {}", new DecimalFormat("#.##").format(progress / maxProgress * 100), message);
                                }
                            }))
                    {
                        TextureResolution blockSettings = blockResources.getTextureResolution();
                        try (SpecularFitOptimizable<ContextType> blockOptimization = SpecularFitOptimizable.createNew(
                            blockResources, programFactory, blockSettings, settings.getGamma(), settings.getSpecularBasisSettings(),
                            settings.getNormalOptimizationSettings(), settings.shouldIncludeConstantTerm()))
                        {
                            if (inputNormalMapFile != null)
                            {
                                int x = settings.getImageCacheSettings().getBlockStartX(i);
                                int y = settings.getImageCacheSettings().getBlockStartY(j);
                                int width = settings.getImageCacheSettings().getBlockStartX(i + 1) - x;
                                int height = settings.getImageCacheSettings().getBlockStartY(j + 1) - y;

                                blockOptimization.getNormalOptimization().getNormalMap().blitCropped(
                                    initialNormalMap, x, y, width, height);
                            }

                            // Use basis functions previously optimized at a lower resolution
                            SpecularDecomposition blockDecomposition =
                                new SpecularDecompositionFromExistingBasis(blockSettings, sampledDecomposition);

                            if (sampledDecomposition.getSpecularBasis().getCount() == 1)
                            {
                                // special case for a single basis function: pre-fill with default weights so that optimization is unnecesssary.
                                int weightCount = blockSettings.width * blockSettings.height;
                                for (int p = 0; p < weightCount; p++)
                                {
                                    blockDecomposition.setWeights(p, SimpleMatrix.identity(1));
                                    blockDecomposition.setWeightsValidity(p, true);
                                }
                            }

                            // Optimize weights and normals
                            optimizeTexSpaceFit(blockResources,
                                (stream, monitorLocal) -> blockOptimization.optimizeFromExistingBasis(
                                    blockDecomposition, stream, settings.getConvergenceTolerance(), monitorLocal,
                                    TRACE_IMAGES && settings.getOutputDirectory() != null ? settings.getOutputDirectory() : null),
                                blockOptimization,
                                new DefaultProgressMonitor() // wrap progress monitor with logic to account for it being just one block out of the whole.
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
                                        if (monitor != null)
                                        {
                                            monitor.setProgress(blockProgress + progress / maxProgress, message);
                                        }
                                    }

                                    @Override
                                    public void complete()
                                    {
                                        if (monitor != null)
                                        {
                                            monitor.setProgress(blockProgress + 1, "Block complete.");
                                        }
                                    }

                                    @Override
                                    public void fail(Throwable e)
                                    {
                                        if (monitor != null)
                                        {
                                            monitor.fail(e);
                                        }
                                    }
                                }
                            );

                            // Fill holes in the weight map
                            blockDecomposition.fillHoles();

                            // Update the GPU resources with the hole-filled weight maps.
                            blockOptimization.getBasisWeightResources().updateFromSolution(blockDecomposition);

                            // Calculate final diffuse map without the constraint of basis functions.
                            blockOptimization.getDiffuseOptimization().execute(blockOptimization);

                            // Fit specular textures after filling holes
                            blockOptimization.getRoughnessOptimization().execute(settings.getGamma());

                            // Copy partial solution into the full solution.
                            fullResolutionDestination.blit(cache.getSettings().getBlockStartX(i), cache.getSettings().getBlockStartY(j), blockOptimization);
                        }
                    }
                }
            }
        }
    }

    private <ContextType extends Context<ContextType>> void optimizeTexSpaceFit(
        IBRResourcesTextureSpace<ContextType> resources,
        OptimizationMethod<ContextType> optimizationMethod,
        SpecularMaterialResources<ContextType> resultsForErrorCalc, ProgressMonitor monitor) throws IOException, UserCancellationException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        // Create new texture fit settings with a resolution that matches the IBRResources, but the same gamma as specified by the user.
        TextureResolution texFitSettings = resources.getTextureResolution();

        try
        (
            // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
            GraphicsStreamResource<ContextType> stream = resources.streamFactory().streamAsResource(
                getReflectanceProgramBuilder(resources, programFactory),
                resources.getContext().buildFramebufferObject(texFitSettings.width, texFitSettings.height)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addColorAttachment(ColorFormat.RGBA32F));
        )
        {
            optimizationMethod.optimize(stream, monitor);
        }
    }

//    /**
//     * Skips most optimization steps and just loads from a prior solution.
//     * Does re-run the GGX fitting step.
//     * @param priorSolutionDirectory The directory containing the prior solution
//     * @param <ContextType> The type of the graphics context
//     * @return A fit based on the solution loaded from file.
//     * @throws IOException
//     */
//    public <ContextType extends Context<ContextType>> SpecularMaterialResources<ContextType> loadPriorSolution(
//        ContextType context, File priorSolutionDirectory)
//        throws IOException
//    {
//        // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
//        // Only basis representation and diffuse map will be loaded from solution.
//        SpecularFitBase<ContextType> solution = SpecularFitFinal.loadFromPriorSolution(context, priorSolutionDirectory);
//
//        // Fit specular textures
//        solution.getRoughnessOptimization().execute(settings.getGamma());
//        solution.getRoughnessOptimization().saveTextures(settings.getOutputDirectory());
//
//        // Generate albedo / ORM maps
//        try(AlbedoORMOptimization<ContextType> albedoORM = /* TODO: load occlusion map from Metashape project if this function continues to be needed */
//                AlbedoORMOptimization.createWithoutOcclusion(context, settings.getTextureResolution()))
//        {
//            albedoORM.execute(solution, settings.getGamma());
//            albedoORM.saveTextures(settings.getOutputDirectory());
//            return solution;
//        }
//    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getReflectanceProgramBuilder(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
            new File("shaders/common/texspace_dynamic.vert"),
            new File("shaders/specularfit/extractReflectance.frag"));
    }

    public void rescaleTextures()
    {
        if (settings.getExportSettings().isGenerateLowResTextures() && settings.getOutputDirectory() != null)
        {
            SpecularFitTextureRescaler rescaler = new SpecularFitTextureRescaler(settings.getExportSettings());
            rescaler.rescaleAll(settings.getOutputDirectory(), settings.getSpecularBasisSettings().getBasisCount());

            if (settings.shouldIncludeConstantTerm())
            {
                try
                {
                    rescaler.generateLodsFor(new File(settings.getOutputDirectory(), "constant.png"));
                }
                catch (IOException e)
                {
                    log.error("Failed to resize diffuse constant texture", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
