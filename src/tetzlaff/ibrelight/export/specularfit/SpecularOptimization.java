/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
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

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.rendering.resources.GraphicsStream;
import tetzlaff.ibrelight.rendering.resources.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
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

    public <ContextType extends Context<ContextType>> SpecularFit<ContextType> createFit(IBRResources<ContextType> resources)
        throws IOException
    {
        // Get GPU context and disable back face culling since we're rendering in texture space
        ContextType context = resources.context;
        context.getState().disableBackFaceCulling();
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(resources, settings);

        // Calculate reasonable image resolution for error calculation
        Projection defaultProj = resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(
            resources.viewSet.getPrimaryViewIndex()));

        int imageWidth;
        int imageHeight;

        if (defaultProj.getAspectRatio() < 1.0)
        {
            imageWidth = settings.width;
            imageHeight = Math.round(imageWidth / defaultProj.getAspectRatio());
        }
        else
        {
            imageHeight = settings.height;
            imageWidth = Math.round(imageHeight * defaultProj.getAspectRatio());
        }

        // Create space for the solution.
        SpecularFitSolution solution = new SpecularFitSolution(settings);

        // Initialize weights using K-means.
        new SpecularFitInitializer<>(resources, settings).initialize(solution);

        // Complete "specular fit": includes basis representation on GPU, roughness / reflectivity fit, normal fit, and final diffuse fit.
        SpecularFit<ContextType> specularFit = new SpecularFit<>(context, resources, settings);

        try
        (
            // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
            GraphicsStreamResource<ContextType> reflectanceStream = resources.streamAsResource(
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
                    x -> 3*x*x-2*x*x*x));
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
                }

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
                specularFit.roughnessOptimization.execute();

                if (DEBUG)
                {
                    specularFit.roughnessOptimization.saveTextures();
                }
            }
            while (previousIterationError - errorCalculator.getReport().getError() > settings.getConvergenceTolerance());

            // Save the final basis functions
            solution.saveBasisFunctions();

            // Save the final normal map
            specularFit.normalOptimization.saveNormalMap();

            // Calculate final diffuse map without the constraint of basis functions.
            specularFit.diffuseOptimization.execute(specularFit);
            specularFit.diffuseOptimization.saveDiffuseMap();

            // Save basis image visualization for reference and debugging
            try(BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(context, settings))
            {
                basisImageCreator.createImages(specularFit);
            }

            // Fill holes in weight maps and calculate some final error statistics.
            new SpecularFitFinalizer(settings)
                .execute(solution, resources, specularFit, scratchFramebuffer, errorCalculator.getReport(), errorCalcDrawable);

            return specularFit;
        }
    }

    private static void logError(ReadonlyErrorReport report)
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
