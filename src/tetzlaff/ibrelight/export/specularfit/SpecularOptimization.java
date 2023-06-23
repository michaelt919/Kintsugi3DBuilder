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
import java.util.function.Function;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.resources.*;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedErrorCalculator;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularOptimization
{
    static final boolean DEBUG = false;

    private final SpecularFitRequestParams settings;

    public SpecularOptimization(SpecularFitRequestParams settings)
    {
        this.settings = settings;
    }

    private int determineImageWidth(ViewSet viewSet)
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

    private int determineImageHeight(ViewSet viewSet)
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


    public <ContextType extends Context<ContextType>> SpecularResources<ContextType> createFit(IBRResourcesImageSpace<ContextType> resources)
        throws IOException
    {
        Instant start = Instant.now();

        // Get GPU context
        ContextType context = resources.getContext();

        // Generate cache
        ImageCache<ContextType> cache = resources.cache(settings.getImageCacheSettings());
        cache.initialize(resources.getViewSet().getImageFilePath() /* TODO: Add support for high-res directory */);

        // Disable back face culling since we're rendering in texture space
        // (should be the case already from generating the cache, but good to do just in case)
        context.getState().disableBackFaceCulling();
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(resources,
                settings.getIbrSettings(), settings.getSpecularBasisSettings());

        // Calculate reasonable image resolution for error calculation
        int imageWidth = determineImageWidth(resources.getViewSet());
        int imageHeight = determineImageHeight(resources.getViewSet());

        try (IBRResources<ContextType> sampled = cache.createSampledResources())
        {
            // Create space for the solution.
            SpecularDecomposition specularDecomposition = new SpecularDecomposition(settings.getTextureFitSettings(), settings.getSpecularBasisSettings());

            // Initialize weights using K-means.
            SpecularFitInitializer<ContextType> initializer = new SpecularFitInitializer<>(resources, settings.getTextureFitSettings(), settings.getSpecularBasisSettings());
            initializer.initialize(programFactory, specularDecomposition);

            if (DEBUG)
            {
                initializer.saveDebugImage(specularDecomposition, settings.getOutputDirectory());
            }

            // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
            SpecularFitFromOptimization<ContextType> specularFit = new SpecularFitFromOptimization<>(context, programFactory,
                settings.getTextureFitSettings(), settings.getSpecularBasisSettings(), settings.getNormalOptimizationSettings());

            try
            (
                // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
                GraphicsStreamResource<ContextType> reflectanceStream = resources.streamFactory().streamAsResource(
                    getReflectanceProgramBuilder(programFactory),
                    context.buildFramebufferObject(settings.getTextureFitSettings().width, settings.getTextureFitSettings().height)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addColorAttachment(ColorFormat.RGBA32F));

                ShaderBasedErrorCalculator<ContextType> errorCalculator = ShaderBasedErrorCalculator.create(context,
                    () -> createErrorCalcProgram(programFactory), program -> createErrorCalcDrawable(resources::createDrawable, specularFit, program),
                    imageWidth, imageHeight);
            )
            {
                // Setup reflectance extraction program
                programFactory.setupShaderProgram(reflectanceStream.getProgram());

                specularFit.optimize(
                    specularDecomposition, reflectanceStream, settings.getWeightBlockSize(), settings.getConvergenceTolerance(),
                    errorCalculator, DEBUG ? settings.getOutputDirectory() : null);

                // Calculate final diffuse map without the constraint of basis functions.
                specularFit.diffuseOptimization.execute(specularFit);

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
                specularFit.diffuseOptimization.saveDiffuseMap(settings.getOutputDirectory());
                specularFit.normalOptimization.saveNormalMap(settings.getOutputDirectory());

                // Save the final basis functions
                specularDecomposition.saveBasisFunctions(settings.getOutputDirectory());

                // Save basis image visualization for reference and debugging
                try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(context, settings.getSpecularBasisSettings()))
                {
                    basisImageCreator.createImages(specularFit, settings.getOutputDirectory());
                }

                try (PrintStream rmseOut = new PrintStream(new File(settings.getOutputDirectory(), "rmse.txt")))
                {
                    SpecularFitFinalizer finalizer = new SpecularFitFinalizer(settings.getTextureFitSettings(), settings.getSpecularBasisSettings());
                    // Validate normals using input normal map (mainly for testing / experiment validation, not typical applications)
                    finalizer.validateNormalMap(resources, specularFit, rmseOut);

                    // Fill holes in weight maps and calculate some final error statistics.
                    finalizer.finishAndCalculateError(specularDecomposition, resources, programFactory, specularFit,
                        errorCalculator, rmseOut, settings.getOutputDirectory());
                }

                // Generate albedo / ORM maps
                try (AlbedoORMOptimization<ContextType> albedoORM = new AlbedoORMOptimization<>(context, settings.getTextureFitSettings()))
                {
                    albedoORM.execute(specularFit);
                    albedoORM.saveTextures(settings.getOutputDirectory());
                    return specularFit;
                }
            }
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
        SpecularFitBase<ContextType> solution = new SpecularFitFromPriorSolution<>(context,
            settings.getTextureFitSettings(), settings.getSpecularBasisSettings(), priorSolutionDirectory);

        // Fit specular textures
        solution.roughnessOptimization.execute();
        solution.roughnessOptimization.saveTextures(settings.getOutputDirectory());

        // Generate albedo / ORM maps
        try(AlbedoORMOptimization<ContextType> albedoORM = new AlbedoORMOptimization<>(context, settings.getTextureFitSettings()))
        {
            albedoORM.execute(solution);
            albedoORM.saveTextures(settings.getOutputDirectory());
        }

        return solution;
    }

    static void logError(ReadonlyErrorReport report)
    {
        System.out.println("--------------------------------------------------");
        System.out.println("Error: " + report.getError());
        System.out.println("(Previous error: " + report.getPreviousError() + ')');
        System.out.println("--------------------------------------------------");
        System.out.println();
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getReflectanceProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/specularfit/extractReflectance.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(SpecularFitProgramFactory<ContextType> programFactory)
    {
        try
        {
            return programFactory.createProgram(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/specularfit/errorCalc.frag"));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType> createErrorCalcDrawable(
            Function<Program<ContextType>, Drawable<ContextType>> drawableFactory,
            SpecularFitFromOptimization<ContextType> specularFit, Program<ContextType> errorCalcProgram)
    {
        Drawable<ContextType> errorCalcDrawable = drawableFactory.apply(errorCalcProgram);
        specularFit.basisResources.useWithShaderProgram(errorCalcProgram);
        errorCalcProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
        errorCalcProgram.setUniform("errorGamma", 1.0f);
        return errorCalcDrawable;
    }
}
