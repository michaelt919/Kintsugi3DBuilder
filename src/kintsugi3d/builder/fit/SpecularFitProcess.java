/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import kintsugi3d.builder.core.TextureFitSettings;
import kintsugi3d.builder.export.specular.SpecularFitTextureRescaler;
import kintsugi3d.builder.export.specular.WeightImageCreator;
import kintsugi3d.builder.fit.debug.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.SpecularDecomposition;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromExistingBasis;
import kintsugi3d.builder.fit.decomposition.SpecularDecompositionFromScratch;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.resources.ibr.*;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamResource;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.optimization.ShaderBasedErrorCalculator;
import kintsugi3d.util.ImageFinder;
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

    public <ContextType extends Context<ContextType>> void optimizeFit(IBRResourcesImageSpace<ContextType> resources)
        throws IOException
    {
        Instant start = Instant.now();

        // Generate cache
        ImageCache<ContextType> cache = resources.cache(settings.getImageCacheSettings());

        Duration duration = Duration.between(start, Instant.now());
        log.info("Cache found / generated in: " + duration);

        // Runs the fit (long process) and then replaces the old material resources / textures
        resources.replaceSpecularMaterialResources(optimizeFit(cache, resources.getSpecularMaterialResources()));

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
        ImageCache<ContextType> cache, SpecularMaterialResources<ContextType> original)
        throws IOException
    {
        Instant start = Instant.now();

        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        // Create space for the solution.
        // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
        SpecularFitFinal<ContextType> fullResolution = SpecularFitFinal.createEmpty(original,
            settings.getTextureFitSettings(), settings.getSpecularBasisSettings(), settings.shouldIncludeConstantTerm());

        try (IBRResourcesTextureSpace<ContextType> sampled = cache.createSampledResources())
        {
            ContextType context = sampled.getContext();
            // Disable back face culling since we're rendering in texture space
            // (should be the case already from generating the cache, but good to do just in case)
            context.getState().disableBackFaceCulling();

            TextureFitSettings sampledSettings = sampled.getTextureFitSettings();
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
                optimizeTexSpaceFit(sampled, (stream, errorCalculator) -> sampledFit.optimizeFromScratch(
                    sampledDecomposition, stream, settings.getPreliminaryConvergenceTolerance(),
                    errorCalculator, TRACE_IMAGES && settings.getOutputDirectory() != null ? settings.getOutputDirectory() : null), sampledFit
                );

                if (settings.getOutputDirectory() != null)
                {
                    // Save the final basis functions
                    sampledDecomposition.saveBasisFunctions(settings.getOutputDirectory());
                }

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

                File geometryFile = sampled.getViewSet().getGeometryFile();
                File inputNormalMap = geometryFile == null ? null :
                    ImageFinder.getInstance()
                        .tryFindImageFile(new File(geometryFile.getParentFile(),
                            sampled.getGeometry().getMaterial().getNormalMap().getMapName()));

                // Optimize weight maps and normal maps by blocks to fill the full resolution textures
                optimizeBlocks(fullResolution, cache, sampledDecomposition, inputNormalMap);
            }

            Duration duration = Duration.between(start, Instant.now());
            log.info("Total processing time: " + duration);

            if (DEBUG_IMAGES && settings.getOutputDirectory() != null)
            {
                try (PrintStream time = new PrintStream(new File(settings.getOutputDirectory(), "time.txt")))
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
                // Save the final diffuse and normal maps
                fullResolution.saveDiffuseMap(settings.getOutputDirectory());
                fullResolution.saveNormalMap(settings.getOutputDirectory());

                if (settings.shouldIncludeConstantTerm())
                {
                    fullResolution.saveConstantMap(settings.getOutputDirectory());
//                fullResolution.saveQuadraticMap(settings.getOutputDirectory());
                }

                // Save the final weight maps
                int weightsPerImage = settings.getExportSettings().isCombineWeights() ? 4 : 1;
                try (WeightImageCreator<ContextType> weightImageCreator = new WeightImageCreator<>(cache.getContext(), settings.getTextureFitSettings(), weightsPerImage))
                {
                    weightImageCreator.createImages(fullResolution, settings.getOutputDirectory());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }

                // Save the final specular albedo and roughness maps
                fullResolution.getRoughnessOptimization().saveTextures(settings.getOutputDirectory());

                // Generate albedo / ORM maps at full resolution (does not require loaded source images)
                fullResolution.getAlbedoORMOptimization().execute(fullResolution, settings.getGamma());
                fullResolution.getAlbedoORMOptimization().saveTextures(settings.getOutputDirectory());
            }

            return fullResolution;
        }
        catch (Exception e)
        {
            // Prevent memory leak when an exception occurs
            fullResolution.close();
            throw e;
        }
    }

    private <ContextType extends Context<ContextType>> void optimizeBlocks(
        Blittable<SpecularMaterialResources<ContextType>> fullResolutionDestination,
        ImageCache<ContextType> cache,
        SpecularDecompositionFromScratch sampledDecomposition,
        File inputNormalMapFile)
        throws IOException
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

            // Optimize each block of the texture map at full resolution.
            for (int i = 0; i < cache.getSettings().getTextureSubdiv(); i++)
            {
                for (int j = 0; j < cache.getSettings().getTextureSubdiv(); j++)
                {
                    log.info("Starting block (" + i + ", " + j + ")...");

                    try (IBRResourcesTextureSpace<ContextType> blockResources = blockResourceFactory.createBlockResources(i, j))
                    {
                        TextureFitSettings blockSettings = blockResources.getTextureFitSettings();
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

                            // Optimize weights and normals
                            optimizeTexSpaceFit(blockResources, (stream, errorCalculator) -> blockOptimization.optimizeFromExistingBasis(
                                blockDecomposition, stream, settings.getConvergenceTolerance(),
                                errorCalculator, TRACE_IMAGES && settings.getOutputDirectory() != null ? settings.getOutputDirectory() : null), blockOptimization
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
        BiConsumer<GraphicsStreamResource<ContextType>, ShaderBasedErrorCalculator<ContextType>> optimizeFunc,
        SpecularMaterialResources<ContextType> resultsForErrorCalc) throws IOException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        // Create new texture fit settings with a resolution that matches the IBRResources, but the same gamma as specified by the user.
        TextureFitSettings texFitSettings = resources.getTextureFitSettings();

        try
        (
            // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
            GraphicsStreamResource<ContextType> stream = resources.streamFactory().streamAsResource(
                getReflectanceProgramBuilder(resources, programFactory),
                resources.getContext().buildFramebufferObject(texFitSettings.width, texFitSettings.height)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addColorAttachment(ColorFormat.RGBA32F));

            ShaderBasedErrorCalculator<ContextType> errorCalculator = ShaderBasedErrorCalculator.create(resources.getContext(),
                () -> createErrorCalcProgram(resources, programFactory),
                program -> createErrorCalcDrawable(resultsForErrorCalc, resources, program),
                texFitSettings.width, texFitSettings.height)
        )
        {
            optimizeFunc.accept(stream, errorCalculator);
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
//                AlbedoORMOptimization.createWithoutOcclusion(context, settings.getTextureFitSettings()))
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

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createErrorCalcProgram(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        try
        {
            return programFactory.createProgram(resources,
                new File("shaders/common/texspace_dynamic.vert"),
                new File("shaders/specularfit/errorCalc.frag"));
        }
        catch (FileNotFoundException e)
        {
            log.error("An error occurred creating error calculation shader:", e);
            return null;
        }
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType> createErrorCalcDrawable(
            SpecularMaterialResources<ContextType> specularFit, ReadonlyIBRResources<ContextType> resources, Program<ContextType> errorCalcProgram)
    {
        Drawable<ContextType> errorCalcDrawable = resources.createDrawable(errorCalcProgram);
        specularFit.getBasisResources().useWithShaderProgram(errorCalcProgram);
        specularFit.getBasisWeightResources().useWithShaderProgram(errorCalcProgram);
        errorCalcProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
        errorCalcProgram.setTexture("normalEstimate", specularFit.getNormalMap());
        errorCalcProgram.setUniform("errorGamma", 1.0f);
        return errorCalcDrawable;
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
