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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import kintsugi3d.builder.core.*;
import kintsugi3d.builder.export.specular.SpecularFitTextureRescaler;
import kintsugi3d.builder.fit.debug.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.SpecularDecomposition;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromExistingBasis;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromScratch;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.rendering.ImageReconstruction;
import kintsugi3d.builder.rendering.ReconstructionView;
import kintsugi3d.builder.resources.ibr.*;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamResource;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.material.ReadonlyMaterial;
import kintsugi3d.gl.material.ReadonlyMaterialTextureMap;
import kintsugi3d.optimization.ShaderBasedErrorCalculator;
import kintsugi3d.util.BufferedImageColorList;
import kintsugi3d.util.ImageFinder;
import kintsugi3d.util.ImageUndistorter;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.imageio.ImageIO.read;

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

    public <ContextType extends Context<ContextType>> void optimizeFitWithCache(
        IBRResourcesCacheable<ContextType> resources, ProgressMonitor monitor)
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
        resources.replaceSpecularMaterialResources(optimizeFitWithCache(cache, resources.getSpecularMaterialResources(), monitor));

//        // Save basis image visualization for reference and debugging
//        try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(cache.getContext(), settings.getSpecularBasisSettings()))
//        {
//            basisImageCreator.createImages(specularFit, settings.getOutputDirectory());
//        }
    }

    public <ContextType extends Context<ContextType>> void reconstructAll(
        IBRResources<ContextType> resources, BiConsumer<ReconstructionView<ContextType>, ColorAppearanceRMSE> reconstructionCallback)
        throws IOException
    {
        ViewSet viewSet = resources.getViewSet();
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(settings.getIbrSettings(), settings.getSpecularBasisSettings());
        try(ImageReconstruction<ContextType> reconstruction = new ImageReconstruction<>(
            viewSet,
            builder -> builder
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment(),
            builder -> builder
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment(),
            ReconstructionShaders.getIncidentRadianceProgramBuilder(resources, programFactory),
            resources,
            viewIndex ->
            {
                try
                {
                    Projection projection = resources.getViewSet().getCameraProjection(resources.getViewSet().getCameraProjectionIndex(viewIndex));
                    BufferedImage image = read(viewSet.findFullResImageFile(viewIndex));

                    if (projection instanceof DistortionProjection)
                    {
                        // undistort if we have a DistortionProjection.
                        return new BufferedImageColorList(new ImageUndistorter<>(resources.getContext())
                            .undistort(image, false /* no mipmaps for error estimation */, (DistortionProjection)projection));
                    }
                    else
                    {
                        return new BufferedImageColorList(image);
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });
            ProgramObject<ContextType> basisModelReconstructionProgram = ReconstructionShaders.getBasisModelReconstructionProgramBuilder(
                    resources, resources.getSpecularMaterialResources(), programFactory)
                .createProgram();
            Drawable<ContextType> drawable = resources.createDrawable(basisModelReconstructionProgram))
        {
            resources.setupShaderProgram(basisModelReconstructionProgram);

            for (ReconstructionView<ContextType> view : reconstruction)
            {
                ColorAppearanceRMSE rmse = view.reconstruct(drawable);
                reconstructionCallback.accept(view, rmse);
            }
        }

        rescaleTextures();
    }

    public <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> optimizeFit(
        IBRResources<ContextType> resources, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        SpecularDecompositionFromScratch decomposition =
            new SpecularDecompositionFromScratch(settings.getTextureResolution(), settings.getSpecularBasisSettings());
        return optimizeFit(resources, decomposition, monitor);
    }

    private <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> optimizeFit(
        IBRResources<ContextType> resources, SpecularDecompositionFromScratch decomposition, ProgressMonitor monitor)
        throws IOException, UserCancellationException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        // Initialize weights using K-means.
        SpecularFitInitializer<ContextType> initializer = new SpecularFitInitializer<>(resources, settings.getSpecularBasisSettings());
        initializer.initialize(programFactory, decomposition);

        if (DEBUG_IMAGES && settings.getOutputDirectory() != null)
        {
            initializer.saveDebugImage(decomposition, settings.getOutputDirectory());
        }

        SpecularFitOptimizable<ContextType> specularFit = SpecularFitOptimizable.createNew(
            resources, programFactory, decomposition.getTextureResolution(), settings.getSpecularBasisSettings(),
            settings.getNormalOptimizationSettings(), false);

        try
        {
            // Preliminary optimization at low resolution to determine basis functions
            this.optimizeTexSpaceFit(resources, decomposition.getTextureResolution(),
                (stream, monitorLocal) -> specularFit.optimizeFromScratch(
                    decomposition, stream, settings.getPreliminaryConvergenceTolerance(),
                    monitorLocal, TRACE_IMAGES && settings.getOutputDirectory() != null ? settings.getOutputDirectory() : null),
                specularFit, monitor);

//            if (settings.getOutputDirectory() != null)
//            {
//                // Save the final basis functions
//                decomposition.saveBasisFunctions(settings.getOutputDirectory());
//            }

            if (DEBUG_IMAGES && settings.getOutputDirectory() != null)
            {
                // write out diffuse texture for debugging
                decomposition.saveDiffuseMap(settings.getOutputDirectory());

                // Save basis image visualization for reference and debugging
                try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(resources.getContext(), settings.getSpecularBasisSettings()))
                {
                    basisImageCreator.createImages(specularFit, settings.getOutputDirectory());
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

    private <ContextType extends Context<ContextType>> SpecularMaterialResources<ContextType> optimizeFitWithCache(
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

            SpecularDecompositionFromScratch sampledDecomposition =
                new SpecularDecompositionFromScratch(sampled.getTextureResolution(), settings.getSpecularBasisSettings());

            try (SpecularFitOptimizable<ContextType> sampledFit = optimizeFit(sampled, sampledDecomposition, monitor)) // low-res fit happens here; takes a while
            {
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
            fullResolution.getAlbedoORMOptimization().execute(fullResolution);

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
                        TextureResolution blockResolution = blockResources.getTextureResolution();
                        try (SpecularFitOptimizable<ContextType> blockOptimization = SpecularFitOptimizable.createNew(
                            blockResources, programFactory, blockResolution, settings.getSpecularBasisSettings(),
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
                                new SpecularDecompositionFromExistingBasis(blockResolution, sampledDecomposition);

                            if (sampledDecomposition.getMaterialBasis().getMaterialCount() == 1)
                            {
                                // special case for a single basis function: pre-fill with default weights so that optimization is unnecesssary.
                                int weightCount = blockResolution.width * blockResolution.height;
                                for (int p = 0; p < weightCount; p++)
                                {
                                    blockDecomposition.setWeights(p, SimpleMatrix.identity(1));
                                    blockDecomposition.setWeightsValidity(p, true);
                                }
                            }

                            // Optimize weights and normals
                            optimizeTexSpaceFit(blockResources,
                                blockResolution,
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
                            blockOptimization.getRoughnessOptimization().execute();

                            // Copy partial solution into the full solution.
                            fullResolutionDestination.blit(cache.getSettings().getBlockStartX(i), cache.getSettings().getBlockStartY(j), blockOptimization);
                        }
                    }
                }
            }
        }
    }

    private <ContextType extends Context<ContextType>> void optimizeTexSpaceFit(
        IBRResources<ContextType> resources,
        TextureResolution resolution,
        OptimizationMethod<ContextType> optimizationMethod,
        SpecularMaterialResources<ContextType> resultsForErrorCalc, ProgressMonitor monitor) throws IOException, UserCancellationException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        try
        (
            // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
            GraphicsStreamResource<ContextType> stream = resources.streamFactory().streamAsResource(
                getReflectanceProgramBuilder(resources, programFactory),
                resources.getContext().buildFramebufferObject(resolution.width, resolution.height)
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
