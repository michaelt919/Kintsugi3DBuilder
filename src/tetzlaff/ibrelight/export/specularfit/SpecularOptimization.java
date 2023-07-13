/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.core.ReadonlyViewSet;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.export.specularfit.gltf.SpecularFitGltfExporter;
import tetzlaff.ibrelight.rendering.resources.*;
import tetzlaff.optimization.ShaderBasedErrorCalculator;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularOptimization
{
    static final boolean DEBUG = true;

    private final SpecularFitRequestParams settings;

    public SpecularOptimization(SpecularFitRequestParams settings)
    {
        this.settings = settings;
    }

    private int determineImageWidth(ReadonlyViewSet viewSet)
    {
        Projection defaultProj = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(
            viewSet.getPrimaryViewIndex()));

        if (defaultProj.getAspectRatio() < 1.0)
        {
            return settings.getTextureFitSettings().width;
        }
        else
        {
            return Math.round(settings.getTextureFitSettings().height * defaultProj.getAspectRatio());
        }
    }

    private int determineImageHeight(ReadonlyViewSet viewSet)
    {
        Projection defaultProj = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(
            viewSet.getPrimaryViewIndex()));

        if (defaultProj.getAspectRatio() < 1.0)
        {
            return Math.round(settings.getTextureFitSettings().width / defaultProj.getAspectRatio());
        }
        else
        {
            return settings.getTextureFitSettings().height;
        }
    }

    private <ContextType extends Context<ContextType>> SpecularFitProgramFactory<ContextType> getProgramFactory()
    {
        return new SpecularFitProgramFactory<>(
            settings.getIbrSettings(), settings.getSpecularBasisSettings());
    }

    public <ContextType extends Context<ContextType>> SpecularResources<ContextType> createFit(IBRResourcesImageSpace<ContextType> resources)
        throws IOException
    {
        Instant start = Instant.now();

        // Generate cache
        ImageCache<ContextType> cache = resources.cache(settings.getImageCacheSettings());

        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Cache generated / loaded in: " + duration);

        SpecularResources<ContextType> specularFit = createFit(cache);

        // Save basis image visualization for reference and debugging
        try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(cache.getContext(), settings.getSpecularBasisSettings()))
        {
            basisImageCreator.createImages(specularFit, settings.getOutputDirectory());
        }

        // Calculate reasonable image resolution for error calculation
        int imageWidth = determineImageWidth(resources.getViewSet());
        int imageHeight = determineImageHeight(resources.getViewSet());

        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        try (ShaderBasedErrorCalculator<ContextType> errorCalculator =
             ShaderBasedErrorCalculator.create(
                cache.getContext(),
                () -> createErrorCalcProgram(resources, programFactory),
                program -> createErrorCalcDrawable(specularFit, resources, program),
                imageWidth, imageHeight);
             PrintStream rmseOut = new PrintStream(new File(settings.getOutputDirectory(), "rmse.txt")))
        {
            FinalErrorCalculaton finalErrorCalculaton = FinalErrorCalculaton.getInstance();

            // Validate normals using input normal map (mainly for testing / experiment validation, not typical applications)
            finalErrorCalculaton.validateNormalMap(resources, specularFit, rmseOut);

            // Fill holes in weight maps and calculate some final error statistics.
            finalErrorCalculaton.calculateFinalErrorMetrics(resources, programFactory, specularFit, errorCalculator, rmseOut);
        }

        // Generate albedo / ORM maps at full resolution (does not require loaded source images)
        try (AlbedoORMOptimization<ContextType> albedoORM = new AlbedoORMOptimization<>(cache.getContext(), settings.getTextureFitSettings()))
        {
            albedoORM.execute(specularFit);
            albedoORM.saveTextures(settings.getOutputDirectory());
        }

        rescaleTextures();

        if (settings.getExportSettings().isGlTFEnabled())
        {
            saveGlTF(resources);
        }

        return specularFit;
    }

    public <ContextType extends Context<ContextType>> SpecularResources<ContextType> createFit(ImageCache<ContextType> cache)
        throws IOException
    {
        Instant start = Instant.now();

        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        // Create space for the solution.
        // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
        SpecularFitFinal<ContextType> fullResolution = SpecularFitFinal.createEmpty(cache.getContext(),
            settings.getTextureFitSettings(), settings.getSpecularBasisSettings());

        try (IBRResourcesTextureSpace<ContextType> sampled = cache.createSampledResources())
        {
            ContextType context = sampled.getContext();
            // Disable back face culling since we're rendering in texture space
            // (should be the case already from generating the cache, but good to do just in case)
            context.getState().disableBackFaceCulling();

            TextureFitSettings sampledSettings = sampled.getTextureFitSettings(settings.getTextureFitSettings().gamma);
            SpecularDecompositionFromScratch sampledDecomposition = new SpecularDecompositionFromScratch(sampledSettings, settings.getSpecularBasisSettings());

            // Initialize weights using K-means.
            SpecularFitInitializer<ContextType> initializer = new SpecularFitInitializer<>(sampled, settings.getSpecularBasisSettings());
            initializer.initialize(programFactory, sampledDecomposition);

            if (DEBUG)
            {
                initializer.saveDebugImage(sampledDecomposition, settings.getOutputDirectory());
            }

            try
            (
                SpecularFitOptimizable<ContextType> sampledFit = SpecularFitOptimizable.create(
                    sampled, programFactory, sampledSettings, settings.getSpecularBasisSettings(), settings.getNormalOptimizationSettings())
            )
            {
                // Preliminary optimization at low resolution to determine basis functions
                optimizeTexSpaceFit(sampled, sampledFit,
                    (stream, errorCalculator) -> sampledFit.optimizeFromScratch(
                        sampledDecomposition, stream, settings.getPreliminaryConvergenceTolerance(),
                        errorCalculator, DEBUG ? settings.getOutputDirectory() : null));

                // Save the final basis functions
                sampledDecomposition.saveBasisFunctions(settings.getOutputDirectory());

                // Optimize weight maps and normal maps by blocks to fill the full resolution textures
                optimizeBlocks(fullResolution, cache, sampledDecomposition);
            }

            Duration duration = Duration.between(start, Instant.now());
            System.out.println("Total processing time: " + duration);

            try (PrintStream time = new PrintStream(new File(settings.getOutputDirectory(), "time.txt")))
            {
                time.println(duration);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }

            // Save the final diffuse and normal maps
            fullResolution.saveDiffuseMap(settings.getOutputDirectory());
            fullResolution.saveNormalMap(settings.getOutputDirectory());

            // Save the final weight maps
            // TODO combined weight images when using block-based fitting
            SpecularFitSerializer.saveWeightImages(fullResolution.getBasisWeightResources().weightMaps, settings.getOutputDirectory());

            // Save the final specular albedo and roughness maps
            fullResolution.getRoughnessOptimization().saveTextures(settings.getOutputDirectory());

            return fullResolution;
        }
    }

    private <ContextType extends Context<ContextType>> void optimizeBlocks(
        SpecularFitFinal<ContextType> fullResolutionSolution,
        ImageCache<ContextType> cache,
        SpecularDecompositionFromScratch sampledDecomposition)
        throws IOException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        try(TextureBlockResourceFactory<ContextType> blockResourceFactory = cache.createBlockResourceFactory())
        {
            // Optimize each block of the texture map at full resolution.
            for (int i = 0; i < cache.getSettings().getTextureSubdiv(); i++)
            {
                for (int j = 0; j < cache.getSettings().getTextureSubdiv(); j++)
                {
                    System.out.println();
                    System.out.println("Starting block (" + i + ", " + j + ")...");

                    try (IBRResourcesTextureSpace<ContextType> blockResources = blockResourceFactory.createBlockResources(i, j))
                    {
                        TextureFitSettings blockSettings = blockResources.getTextureFitSettings(settings.getTextureFitSettings().gamma);
                        try (SpecularFitOptimizable<ContextType> blockOptimization = SpecularFitOptimizable.create(
                            blockResources, programFactory, blockSettings, settings.getSpecularBasisSettings(),
                            settings.getNormalOptimizationSettings()))
                        {
                            // Use basis functions previously optimized at a lower resolution
                            SpecularDecomposition blockDecomposition =
                                new SpecularDecompositionFromExistingBasis(blockSettings, sampledDecomposition);

                            // Optimize weights and normals
                            optimizeTexSpaceFit(blockResources, blockOptimization,
                                (stream, errorCalculator) -> blockOptimization.optimizeFromExistingBasis(
                                    blockDecomposition, stream, settings.getConvergenceTolerance(),
                                    errorCalculator, DEBUG ? settings.getOutputDirectory() : null));

                            // Fill holes in the weight map
                            blockDecomposition.fillHoles();

                            // Update the GPU resources with the hole-filled weight maps.
                            blockOptimization.getBasisWeightResources().updateFromSolution(blockDecomposition);

                            // Calculate final diffuse map without the constraint of basis functions.
                            blockOptimization.getDiffuseOptimization().execute(blockOptimization);

                            // Fit specular textures after filling holes
                            blockOptimization.getRoughnessOptimization().execute();

                            // Copy partial solution into the full solution.
                            fullResolutionSolution.blit(cache.getSettings().getBlockStartX(i), cache.getSettings().getBlockStartY(j), blockOptimization);
                        }
                    }
                }
            }
        }
    }

    private <ContextType extends Context<ContextType>> void optimizeTexSpaceFit(
        IBRResourcesTextureSpace<ContextType> resources,
        SpecularFitOptimizable<ContextType> specularFit,
        BiConsumer<GraphicsStreamResource<ContextType>, ShaderBasedErrorCalculator<ContextType>> optimizeFunc) throws IOException
    {
        SpecularFitProgramFactory<ContextType> programFactory = getProgramFactory();

        // Create new texture fit settings with a resolution that matches the IBRResources, but the same gamma as specified by the user.
        TextureFitSettings texFitSettings = resources.getTextureFitSettings(settings.getTextureFitSettings().gamma);

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
                program -> createErrorCalcDrawable(specularFit, resources, program),
                texFitSettings.width, texFitSettings.height)
        )
        {
            optimizeFunc.accept(stream, errorCalculator);
        }
    }

    /**
     * Skips most optimization steps and just loads from a prior solution.
     * Does re-run the GGX fitting step.
     * @param priorSolutionDirectory The directory containing the prior solution
     * @param <ContextType> The type of the graphics context
     * @return A fit based on the solution loaded from file.
     * @throws IOException
     */
    public <ContextType extends Context<ContextType>> SpecularResources<ContextType> loadPriorSolution(
        ContextType context, File priorSolutionDirectory)
        throws IOException
    {
        // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
        // Only basis representation and diffuse map will be loaded from solution.
        SpecularFitBase<ContextType> solution = SpecularFitFinal.loadFromPriorSolution(context,
            settings.getTextureFitSettings(), settings.getSpecularBasisSettings(), priorSolutionDirectory);

        // Fit specular textures
        solution.getRoughnessOptimization().execute();
        solution.getRoughnessOptimization().saveTextures(settings.getOutputDirectory());

        // Generate albedo / ORM maps
        try(AlbedoORMOptimization<ContextType> albedoORM = new AlbedoORMOptimization<>(context, settings.getTextureFitSettings()))
        {
            albedoORM.execute(solution);
            albedoORM.saveTextures(settings.getOutputDirectory());
            return solution;
        }

    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getReflectanceProgramBuilder(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
            new File("shaders/common/texspace_dynamic.vert"),
            new File("shaders/specularfit/extractReflectance.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(
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
            e.printStackTrace();
            return null;
        }
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType> createErrorCalcDrawable(
            SpecularResources<ContextType> specularFit, ReadonlyIBRResources<ContextType> resources, Program<ContextType> errorCalcProgram)
    {
        Drawable<ContextType> errorCalcDrawable = resources.createDrawable(errorCalcProgram);
        specularFit.getBasisResources().useWithShaderProgram(errorCalcProgram);
        specularFit.getBasisWeightResources().useWithShaderProgram(errorCalcProgram);
        errorCalcProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
        errorCalcProgram.setTexture("normalEstimate", specularFit.getNormalMap());
        errorCalcProgram.setUniform("errorGamma", 1.0f);
        return errorCalcDrawable;
    }

    public <ContextType extends Context<ContextType>> void saveGlTF(ReadonlyIBRResources<ContextType> resources)
    {
        if (resources.getGeometry() == null)
        {
            throw new IllegalArgumentException("Geometry is null; cannot export GLTF.");
        }

        System.out.println("Starting glTF export...");
        try
        {

            Matrix4 rotation = resources.getViewSet().getCameraPose(resources.getViewSet().getPrimaryViewIndex());
            Vector3 translation = rotation.getUpperLeft3x3().times(resources.getGeometry().getCentroid().times(-1.f));
            Matrix4 transform = Matrix4.fromColumns(rotation.getColumn(0), rotation.getColumn(1), rotation.getColumn(2), translation.asVector4(1.0f));

            SpecularFitGltfExporter exporter = SpecularFitGltfExporter.fromVertexGeometry(resources.getGeometry(), transform);
            exporter.setDefaultNames();
            exporter.addWeightImages(settings.getSpecularBasisSettings().getBasisCount(), settings.getExportSettings().isCombineWeights());

            // Deal with LODs if enabled
            if (settings.getExportSettings().isGenerateLowResTextures())
            {
                exporter.addAllDefaultLods(settings.getTextureFitSettings().height,
                    settings.getExportSettings().getMinimumTextureResolution());
                exporter.addWeightImageLods(settings.getSpecularBasisSettings().getBasisCount(),
                    settings.getTextureFitSettings().height, settings.getExportSettings().getMinimumTextureResolution());
            }

            exporter.write(new File(settings.getOutputDirectory(), "model.glb"));
            System.out.println("DONE!");
        }
        catch (IOException e)
        {
            System.out.println("Error occurred during glTF export:");
            e.printStackTrace();
        }
    }

    public void rescaleTextures()
    {
        if (settings.getExportSettings().isGenerateLowResTextures())
        {
            SpecularFitTextureRescaler rescaler = new SpecularFitTextureRescaler(settings.getExportSettings());
            rescaler.rescaleAll(settings.getOutputDirectory(), settings.getSpecularBasisSettings().getBasisCount());
        }
    }
}
