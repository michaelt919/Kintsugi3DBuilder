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

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.resources.*;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedErrorCalculator;
import tetzlaff.optimization.function.GeneralizedSmoothStepBasis;
import tetzlaff.util.ColorList;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularOptimization
{
    static final boolean DEBUG = false;

    private final SpecularFitSettings settings;

    public SpecularOptimization(SpecularFitSettings settings)
    {
        this.settings = settings;
    }

    private int determineImageWidth(ViewSet viewSet)
    {
        Projection defaultProj = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(
            viewSet.getPrimaryViewIndex()));

        if (defaultProj.getAspectRatio() < 1.0)
        {
            return settings.width;
        }
        else
        {
            return Math.round(settings.height * defaultProj.getAspectRatio());
        }
    }

    private int determineImageHeight(ViewSet viewSet)
    {
        Projection defaultProj = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(
            viewSet.getPrimaryViewIndex()));

        if (defaultProj.getAspectRatio() < 1.0)
        {
            return Math.round(settings.width / defaultProj.getAspectRatio());
        }
        else
        {
            return settings.height;
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
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(resources, settings);

        // Calculate reasonable image resolution for error calculation
        int imageWidth = determineImageWidth(resources.getViewSet());
        int imageHeight = determineImageHeight(resources.getViewSet());

        // Create space for the solution.
        SpecularFitSolution solution = new SpecularFitSolution(settings);

        // Initialize weights using K-means.
        new SpecularFitInitializer<>(resources, settings).initialize(solution);

        // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
        SpecularFitFromOptimization<ContextType> specularFit = new SpecularFitFromOptimization<>(context, resources, settings);

        try
        (
            // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
            GraphicsStreamResource<ContextType> reflectanceStream = resources.streamFactory().streamAsResource(
                getReflectanceProgramBuilder(programFactory),
                context.buildFramebufferObject(settings.width, settings.height)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addColorAttachment(ColorFormat.RGBA32F));

            // Compare fitted models against actual photographs
            Program<ContextType> errorCalcProgram = createErrorCalcProgram(programFactory);

            // Framebuffer for calculating error and reconstructing 3D renderings of the object
            FramebufferObject<ContextType> scratchFramebuffer =
            context.buildFramebufferObject(imageWidth, imageHeight)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {
            // Setup reflectance extraction program
            programFactory.setupShaderProgram(reflectanceStream.getProgram());
            reflectanceStream.getProgram().setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());

            Drawable<ContextType> errorCalcDrawable = resources.createDrawable(errorCalcProgram);
            specularFit.basisResources.useWithShaderProgram(errorCalcProgram);
            errorCalcProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
            errorCalcProgram.setUniform("errorGamma", 1.0f);

            // Track how the error improves over iterations of the whole algorithm.
            double previousIterationError;

            BRDFReconstruction brdfReconstruction = new BRDFReconstruction(
                settings,
                new GeneralizedSmoothStepBasis(
                    settings.microfacetDistributionResolution,
                    settings.getMetallicity(),
                    (int)Math.round(settings.getSpecularSmoothness() * settings.microfacetDistributionResolution),
                    x -> 3*x*x-2*x*x*x)
//                new StepBasis(settings.microfacetDistributionResolution, settings.getMetallicity())
            );
            SpecularWeightOptimization weightOptimization = new SpecularWeightOptimization(settings);
            ShaderBasedErrorCalculator errorCalculator = new ShaderBasedErrorCalculator(settings.width * settings.height);

            // Instantiate once so that the memory buffers can be reused.
            GraphicsStream<ColorList[]> reflectanceStreamParallel = reflectanceStream.parallel();

            do
            {
                previousIterationError = errorCalculator.getReport().getError();

                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalEstimate", specularFit.getNormalMap());

                // Reconstruct the basis BRDFs.
                // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
                // Operate in parallel for optimal performance.
                brdfReconstruction.execute(
                    reflectanceStreamParallel.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                    solution);

                // Use the current front normal buffer for calculating error.
                errorCalcProgram.setTexture("normalEstimate", specularFit.getNormalMap());

                // Log error in debug mode.
                if (DEBUG)
                {
                    // Prepare for error calculation on the GPU.
                    // Basis functions will have changed.
                    specularFit.basisResources.updateFromSolution(solution);

                    System.out.println("Calculating error...");
                    errorCalculator.update(errorCalcDrawable, scratchFramebuffer);
                    logError(errorCalculator.getReport());

                    // Save basis image visualization for reference and debugging
                    try(BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(context, settings))
                    {
                        basisImageCreator.createImages(specularFit);
                    }

                    // write out diffuse texture for debugging
                    solution.saveDiffuseMap(settings.additional.getFloat("gamma"));
                }

                if (settings.basisCount > 1)
                {
                    // Make sure there are enough blocks for any pixels that don't go into the weight blocks evenly.
                    int blockCount = (settings.width * settings.height + settings.getWeightBlockSize() - 1) / settings.getWeightBlockSize();

                    // Initially assume that all texels are invalid.
                    solution.invalidateWeights();

                    for (int i = 0; i < blockCount; i++) // TODO: this was done quickly; may need to be refactored
                    {
                        System.out.println("Starting block " + i + "...");
                        weightOptimization.execute(
                            reflectanceStream.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                            solution, i * settings.getWeightBlockSize());
                    }
                }

                if (DEBUG)
                {
                    System.out.println("Calculating error...");
                }

                // Prepare for error calculation and then normal optimization on the GPU.
                // Weight maps will have changed.
                specularFit.basisResources.updateFromSolution(solution);

                // Calculate the error in preparation for normal estimation.
                errorCalculator.update(errorCalcDrawable, scratchFramebuffer);

                if (DEBUG)
                {
                    // Log error in debug mode.
                    logError(errorCalculator.getReport());
                }

                if (settings.isNormalRefinementEnabled())
                {
                    System.out.println("Optimizing normals...");

                    specularFit.normalOptimization.execute(normalMap ->
                    {
                        // Update program to use the new front buffer for error calculation.
                        errorCalcProgram.setTexture("normalEstimate", normalMap);

                        if (DEBUG)
                        {
                            System.out.println("Calculating error...");
                        }

                        // Calculate the error to determine if we should stop.
                        errorCalculator.update(errorCalcDrawable, scratchFramebuffer);

                        if (DEBUG)
                        {
                            // Log error in debug mode.
                            logError(errorCalculator.getReport());
                        }

                        return errorCalculator.getReport();
                    },
                    settings.getConvergenceTolerance());

                    if (errorCalculator.getReport().getError() > errorCalculator.getReport().getPreviousError())
                    {
                        // Revert error calculations to the last accepted result.
                        errorCalculator.reject();
                    }
                }

                // Estimate specular roughness and reflectivity.
                // This can cause error to increase but it's unclear if that poses a problem for convergence.
                specularFit.roughnessOptimization.execute();

                if (DEBUG)
                {
                    specularFit.roughnessOptimization.saveTextures();

                    // Log error in debug mode.
                    specularFit.basisResources.updateFromSolution(solution);
                    System.out.println("Calculating error...");
                    errorCalculator.update(errorCalcDrawable, scratchFramebuffer);
                    logError(errorCalculator.getReport());
                }
            }
            while ((settings.basisCount > 1 || settings.isNormalRefinementEnabled()) &&
                // Iteration not necessary if basisCount is 1 and normal refinement is off.
                previousIterationError - errorCalculator.getReport().getError() > settings.getConvergenceTolerance());

            // Calculate final diffuse map without the constraint of basis functions.
            specularFit.diffuseOptimization.execute(specularFit);

            Duration duration = Duration.between(start, Instant.now());
            System.out.println("Total processing time: " + duration);

            try(PrintStream time = new PrintStream(new File(settings.outputDirectory, "time.txt")))
            {
                time.println(duration);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }

            // Save the final diffuse and normal maps
            specularFit.diffuseOptimization.saveDiffuseMap();
            specularFit.normalOptimization.saveNormalMap();

            // Save the final basis functions
            solution.saveBasisFunctions();

            // Save basis image visualization for reference and debugging
            try(BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(context, settings))
            {
                basisImageCreator.createImages(specularFit);
            }

            try(PrintStream rmseOut = new PrintStream(new File(settings.outputDirectory, "rmse.txt")))
            {
                SpecularFitFinalizer finalizer = new SpecularFitFinalizer(settings);
                // Validate normals using input normal map (mainly for testing / experiment validation, not typical applications)
                finalizer.validateNormalMap(resources, specularFit, rmseOut);

                // Fill holes in weight maps and calculate some final error statistics.
                finalizer.finishAndCalculateError(solution, resources, specularFit, scratchFramebuffer, errorCalculator.getReport(), errorCalcDrawable, rmseOut);
            }

            // Generate albedo / ORM maps
            try(AlbedoORMOptimization<ContextType> albedoORM = new AlbedoORMOptimization<>(context, settings))
            {
                albedoORM.execute(specularFit);
                albedoORM.saveTextures();
            }

            return specularFit;
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
        SpecularFitBase<ContextType> solution = new SpecularFitFromPriorSolution<>(context, settings, priorSolutionDirectory);

        // Fit specular textures
        solution.roughnessOptimization.execute();
        solution.roughnessOptimization.saveTextures();

        // Generate albedo / ORM maps
        try(AlbedoORMOptimization<ContextType> albedoORM = new AlbedoORMOptimization<>(context, settings))
        {
            albedoORM.execute(solution);
            albedoORM.saveTextures();
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
    Program<ContextType> createErrorCalcProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/specularfit/errorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }
}
