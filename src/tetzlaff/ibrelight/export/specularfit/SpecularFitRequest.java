/*
 *  Copyright (c) Michael Tetzlaff 2020
  ~  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.*;
import java.util.*;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.IBRResources;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularFitRequest extends TextureFitRequest
{
    static final boolean DEBUG = true;
    static final boolean ORIGINAL_NAM_METHOD = false;

    static final boolean USE_LEVENBERG_MARQUARDT = !ORIGINAL_NAM_METHOD;
    private static final double CONVERGENCE_TOLERANCE = 0.0001;
    private static final boolean NORMAL_REFINEMENT = true;

    private static final double METALLICITY = 0.0f; // Implemented and minimally tested but doesn't seem to make much difference.

    private final SpecularFitSettings settings;

    public SpecularFitRequest(SpecularFitSettings settings)
    {
        super(settings);

        this.settings = settings;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
    {
        // Get GPU resources and disable back face culling since we're rendering in texture space
        IBRResources<ContextType> resources = renderable.getResources();
        ContextType context = resources.context;
        context.getState().disableBackFaceCulling();
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(resources, settings);

        // Calculate reasonable image resolution for reconstructed images (supplemental output)
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
         new SpecularFitInitializer<>(resources, programFactory, settings).initialize(solution);

        try
        (
            // Reflectance stream: includes a shader program and a framebuffer object for extracting reflectance data from images.
            GraphicsStreamResource<ContextType> reflectanceStream = resources.streamAsResource(
                () -> getReflectanceProgramBuilder(programFactory),
                () -> context.buildFramebufferObject(settings.width, settings.height)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addColorAttachment(ColorFormat.RGBA32F));

            // Compare fitted models against actual photographs
            Program<ContextType> errorCalcProgram = createErrorCalcProgram(programFactory);

            // Rectangle vertex buffer
            VertexBuffer<ContextType> rect = context.createRectangle();

            // Framebuffer for calculating error and reconstructing 3D renderings of the object
            FramebufferObject<ContextType> tempFramebuffer =
                context.buildFramebufferObject(imageWidth, imageHeight)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addDepthAttachment()
                    .createFramebufferObject();

            // Resources specific to this technique:

            // Normal estimation program
            Program<ContextType> normalEstimationProgram = createNormalEstimationProgram(programFactory);

            // Fit specular parameters from weighted basis functions
            Program<ContextType> specularRoughnessFitProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularRoughnessFit.frag"))
                .define("BASIS_COUNT", settings.basisCount)
                .define("MICROFACET_DISTRIBUTION_RESOLUTION", settings.microfacetDistributionResolution)
                .createProgram();

            // Framebuffers (double-buffered) for estimating normals on the GPU
            FramebufferObject<ContextType> normalFramebuffer1 = context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F) // Damping factor while fitting
                .createFramebufferObject();
            FramebufferObject<ContextType> normalFramebuffer2 = context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F) // Damping factor while fitting
                .createFramebufferObject();

            // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
            FramebufferObject<ContextType> specularTexFramebuffer = context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
                .createFramebufferObject();

            // Textures calculated on CPU and passed to GPU (not framebuffers)
            SpecularFitResources<ContextType> specularFitResources = new SpecularFitResources<>(context, settings)
        )
        {
            // Setup reflectance extraction program
            programFactory.setupShaderProgram(reflectanceStream.getProgram());

            normalFramebuffer1.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
            normalFramebuffer2.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            // Double buffering since we need the previous normal estimate to generate the next normal estimate.
            FramebufferObject<ContextType> frontNormalFramebuffer = normalFramebuffer1;
            FramebufferObject<ContextType> backNormalFramebuffer = normalFramebuffer2;

            reflectanceStream.getProgram().setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
            reflectanceStream.getProgram().setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));

            Drawable<ContextType> normalEstimationDrawable = resources.createDrawable(normalEstimationProgram);
            specularFitResources.useWithShaderProgram(normalEstimationProgram);

            Drawable<ContextType> errorCalcDrawable = resources.createDrawable(errorCalcProgram);
            specularFitResources.useWithShaderProgram(errorCalcProgram);
            errorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
            errorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            errorCalcProgram.setUniform("errorGamma", 1.0f);

            Drawable<ContextType> specularRoughnessFitDrawable = context.createDrawable(specularRoughnessFitProgram);
            specularRoughnessFitDrawable.addVertexBuffer("position", rect);
            specularFitResources.useWithShaderProgram(specularRoughnessFitProgram);
            specularRoughnessFitProgram.setUniform("gamma", settings.additional.getFloat("gamma"));
            specularRoughnessFitProgram.setUniform("fittingGamma", 1.0f);

            // Set initial assumption for roughness when calculating masking/shadowing.
            specularTexFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f,1.0f);

            for (int i = 0; i < settings.basisCount; i++)
            {
                solution.setDiffuseAlbedo(i, DoubleVector3.ZERO);
            }

            double previousError;

            BRDFReconstruction brdfReconstruction = new BRDFReconstruction(settings, METALLICITY);
            WeightOptimization weightOptimization = new WeightOptimization(settings, METALLICITY);
            ShaderBasedErrorCalculator errorCalculator = new ShaderBasedErrorCalculator(settings.width * settings.height);

            do
            {
                previousError = errorCalculator.getError();

                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));

                // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
                // Operate in parallel for optimal performance.
                brdfReconstruction.execute(
                    reflectanceStream.parallel().map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                    solution);

                // Calculate the error for debugging.
                if (DEBUG)
                {
                    System.out.println("Calculating error...");

                    errorCalculator.update(errorCalcDrawable, tempFramebuffer);

                    System.out.println("--------------------------------------------------");
                    System.out.println("Error: " + errorCalculator.getError());
                    System.out.println("(Previous error: " + errorCalculator.getPreviousError() + ')');
                    System.out.println("--------------------------------------------------");
                    System.out.println();
                }

                weightOptimization.reconstructWeights(
                    reflectanceStream.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                    solution);

                if (DEBUG)
                {
                    System.out.println("Calculating error...");
                }

                // Calculate the error in preparation for normal estimation.
                errorCalculator.update(errorCalcDrawable, tempFramebuffer);

                if (DEBUG)
                {
                    System.out.println("--------------------------------------------------");
                    System.out.println("Error: " + errorCalculator.getError());
                    System.out.println("(Previous error: " + errorCalculator.getPreviousError() + ')');
                    System.out.println("--------------------------------------------------");
                    System.out.println();
                }

                // Prepare for normal estimation on the GPU.
                specularFitResources.updateFromSolution(solution);

                if (NORMAL_REFINEMENT)
                {
                    // Set damping factor to 1.0 initially at each position.
                    frontNormalFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

                    int unsuccessfulIterations = 0;

                    do
                    {
                        // Update normal estimation program to use the new front buffer.
                        normalEstimationProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                        normalEstimationProgram.setTexture("dampingTex", frontNormalFramebuffer.getColorAttachmentTexture(1));

                        // Estimate new normals.
                        reconstructNormals(normalEstimationDrawable, backNormalFramebuffer);

                        if (DEBUG)
                        {
                            saveNormalMap(frontNormalFramebuffer);
                        }

                        // Swap framebuffers for normal map.
                        FramebufferObject<ContextType> tmp = frontNormalFramebuffer;
                        frontNormalFramebuffer = backNormalFramebuffer;
                        backNormalFramebuffer = tmp;

                        // Update program to use the new front buffer for error calculation.
                        errorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));

                        if (DEBUG)
                        {
                            System.out.println("Calculating error...");
                        }

                        // Calculate the error to determine if we should stop.
                        errorCalculator.update(errorCalcDrawable, tempFramebuffer);

                        System.out.println("--------------------------------------------------");
                        System.out.println("Error: " + errorCalculator.getError());

                        if (DEBUG)
                        {
                            System.out.println("(Previous error: " + errorCalculator.getPreviousError() + ')');
                        }
                        else
                        {
                            System.out.println("(Previous error: " + previousError + ')');
                        }

                        System.out.println("--------------------------------------------------");
                        System.out.println();

                        if (errorCalculator.getPreviousError() - errorCalculator.getError() <= CONVERGENCE_TOLERANCE)
                        {
                            unsuccessfulIterations++;

                            if (errorCalculator.getError() > errorCalculator.getPreviousError())
                            {
                                // Error is worse; reject new normal estimate.
                                // Swap normal map framebuffers back to use the old normal map, if the new one isn't better.
                                backNormalFramebuffer = frontNormalFramebuffer;
                                frontNormalFramebuffer = tmp;

                                // Update program to use the old front buffer for error calculation.
                                errorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                            }
                        }
                        else
                        {
                            unsuccessfulIterations = 0;
                        }
                    }
                    while (USE_LEVENBERG_MARQUARDT && errorCalculator.getError() <= errorCalculator.getPreviousError() && unsuccessfulIterations < 8);

                    if (errorCalculator.getError() > errorCalculator.getPreviousError())
                    {
                        // Revert error calculations to the last accepted result.
                        errorCalculator.reject();
                    }
                }

                // Fit specular so that we have a roughness estimate for masking/shadowing.
                specularTexFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f,0.0f);
                specularTexFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f,0.0f);
                specularRoughnessFitDrawable.draw(PrimitiveMode.TRIANGLE_FAN, specularTexFramebuffer);

                try
                {
                    specularTexFramebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "specular.png"));
                    specularTexFramebuffer.saveColorBufferToFile(1, "PNG", new File(settings.outputDirectory, "roughness.png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            while (previousError - errorCalculator.getError() > CONVERGENCE_TOLERANCE);

            // Save the final normal map (finalize will not do this)
            saveNormalMap(frontNormalFramebuffer);

            new SpecularFitFinalizer(settings).finalize(programFactory, resources, rect, tempFramebuffer, specularRoughnessFitDrawable,
                specularTexFramebuffer, errorCalcDrawable, errorCalculator, solution, specularFitResources, frontNormalFramebuffer.getColorAttachmentTexture(0));
        }
        catch(FileNotFoundException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>>
        ProgramBuilder<ContextType> getReflectanceProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/specularfit/extractReflectance.frag"));
    }

    private static <ContextType extends Context<ContextType>>
        Program<ContextType> createNormalEstimationProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/specularfit/estimateNormals.frag"),
            true,
            Collections.singletonMap("USE_LEVENBERG_MARQUARDT", USE_LEVENBERG_MARQUARDT ? 1 : 0));
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
            new File("shaders/colorappearance/imgspace_multi_as_single.vert"),
            new File("shaders/specularfit/errorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }

    private static <ContextType extends Context<ContextType>> void reconstructNormals(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        if (DEBUG)
        {
            System.out.println("Estimating normals...");
        }

        // Clear framebuffer
        framebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

        // Run shader program to fill framebuffer with per-pixel information.
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        if (DEBUG)
        {
            System.out.println("DONE!");
        }
    }

    private <ContextType extends Context<ContextType>> void saveNormalMap(Framebuffer<ContextType> framebuffer)
    {
        try
        {
            framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "normal.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
