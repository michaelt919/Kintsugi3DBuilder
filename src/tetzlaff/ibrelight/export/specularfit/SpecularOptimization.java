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
import tetzlaff.ibrelight.rendering.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedErrorCalculator;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularOptimization
{
    static final boolean DEBUG = true;
    static final boolean ORIGINAL_NAM_METHOD = false;

    private static final boolean NORMAL_REFINEMENT = true;
    private static final double METALLICITY = 0.0f; // Implemented and minimally tested but doesn't seem to make much difference.
    private static final double CONVERGENCE_TOLERANCE = 0.0001;

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
            reflectanceStream.getProgram().setTexture("normalEstimate", specularFit.normalOptimization.getNormalMap());
            reflectanceStream.getProgram().setTexture("roughnessEstimate", specularFit.roughnessOptimization.getRoughnessTexture());

            Drawable<ContextType> errorCalcDrawable = resources.createDrawable(errorCalcProgram);
            specularFit.basisResources.useWithShaderProgram(errorCalcProgram);
            errorCalcProgram.setTexture("normalEstimate", specularFit.normalOptimization.getNormalMap());
            errorCalcProgram.setTexture("roughnessEstimate", specularFit.roughnessOptimization.getRoughnessTexture());
            errorCalcProgram.setUniform("errorGamma", 1.0f);

            // Track how the error improves over iterations of the whole algorithm.
            double previousIterationError;

            BRDFReconstruction brdfReconstruction = new BRDFReconstruction(settings, METALLICITY);
            WeightOptimization weightOptimization = new WeightOptimization(settings, METALLICITY);
            ShaderBasedErrorCalculator errorCalculator = new ShaderBasedErrorCalculator(settings.width * settings.height);

            do
            {
                previousIterationError = errorCalculator.getReport().getError();

                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalEstimate", specularFit.normalOptimization.getNormalMap());

                // Reconstruct the basis BRDFs.
                // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
                // Operate in parallel for optimal performance.
                brdfReconstruction.execute(
                    reflectanceStream.parallel().map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                    solution);

                // Log error in debug mode.
                if (DEBUG)
                {
                    System.out.println("Calculating error...");
                    errorCalculator.update(errorCalcDrawable, scratchFramebuffer);
                    logError(errorCalculator.getReport());
                }

                // Optimize weights.
                weightOptimization.reconstructWeights(
                    reflectanceStream.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                    solution);

                if (DEBUG)
                {
                    System.out.println("Calculating error...");
                }

                // Calculate the error in preparation for normal estimation.
                errorCalculator.update(errorCalcDrawable, scratchFramebuffer);

                if (DEBUG)
                {
                    // Log error in debug mode.
                    logError(errorCalculator.getReport());
                }

                // Prepare for normal estimation on the GPU.
                specularFit.basisResources.updateFromSolution(solution);

                if (NORMAL_REFINEMENT)
                {
                    if (DEBUG)
                    {
                        System.out.println("Estimating normals...");
                    }

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
                        CONVERGENCE_TOLERANCE);

                    if (errorCalculator.getReport().getError() > errorCalculator.getReport().getPreviousError())
                    {
                        // Revert error calculations to the last accepted result.
                        errorCalculator.reject();
                    }
                }

                // Estimate specular roughness and reflectivity.
                specularFit.roughnessOptimization.execute();
                specularFit.roughnessOptimization.saveTextures();
            }
            while (previousIterationError - errorCalculator.getReport().getError() > CONVERGENCE_TOLERANCE);

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

            // Reconstruct images both from basis functions and from fitted roughness
            FinalReconstruction<ContextType> reconstruction = new FinalReconstruction<>(resources, settings);
            reconstruction.reconstruct(specularFit, getImageReconstructionProgramBuilder(programFactory), "reconstructions");
            reconstruction.reconstruct(specularFit, getFittedImageReconstructionProgramBuilder(programFactory), "fitted");

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
            new File("shaders/colorappearance/imgspace_multi_as_single.vert"),
            new File("shaders/specularfit/errorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getImageReconstructionProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
            new File("shaders/common/imgspace.vert"),
            new File("shaders/specularfit/reconstructImage.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getFittedImageReconstructionProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
            new File("shaders/common/imgspace.vert"),
            new File("shaders/specularfit/renderFit.frag"));
    }
}
