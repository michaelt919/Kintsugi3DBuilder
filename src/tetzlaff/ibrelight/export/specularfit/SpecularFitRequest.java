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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.util.NonNegativeLeastSquares;

import static java.lang.Math.PI;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularFitRequest extends TextureFitRequest
{
    private static final boolean DEBUG = true;
    static final boolean ORIGINAL_NAM_METHOD = false;
    static final boolean USE_LEVENBERG_MARQUARDT = !ORIGINAL_NAM_METHOD;

    private static final double K_MEANS_TOLERANCE = 0.0001;
    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;
    private static final double CONVERGENCE_TOLERANCE = 0.0001;
    private static final double GAMMA = 2.2;

    private static final boolean NORMAL_REFINEMENT = true;

    private static final int MAX_RUNNING_THREADS = 5;

    private static final double METALLICITY = 0.0f; // Implemented and minimally tested but doesn't seem to make much difference.

    private ShaderBasedErrorCalculator errorCalculator;

    private final SpecularFitSettings settings;

    private final int brdfMatrixSize;
    private final SimpleMatrix brdfATA;
    private final SimpleMatrix brdfATyRed;
    private final SimpleMatrix brdfATyGreen;
    private final SimpleMatrix brdfATyBlue;

    private final SimpleMatrix[] weightsQTQAugmented;
    private final SimpleMatrix[] weightsQTrAugmented;

    private final SpecularFitSolution solution;

    private final Object threadsRunningLock = new Object();
    private int threadsRunning = 0;

    public SpecularFitRequest(SpecularFitSettings settings)
    {
        super(settings);

        this.settings = settings;
        solution = new SpecularFitSolution(settings);

        errorCalculator = new ShaderBasedErrorCalculator(settings.width * settings.height);

        brdfMatrixSize = settings.basisCount * (settings.microfacetDistributionResolution + 1);

        brdfATA = new SimpleMatrix(brdfMatrixSize, brdfMatrixSize, DMatrixRMaj.class);
        brdfATyRed = new SimpleMatrix(brdfMatrixSize, 1, DMatrixRMaj.class);
        brdfATyGreen = new SimpleMatrix(brdfMatrixSize, 1, DMatrixRMaj.class);
        brdfATyBlue = new SimpleMatrix(brdfMatrixSize, 1, DMatrixRMaj.class);

        weightsQTQAugmented = IntStream.range(0, settings.width * settings.height)
            .mapToObj(p ->
            {
                SimpleMatrix mQTQAugmented = new SimpleMatrix(settings.basisCount + 1, settings.basisCount + 1, DMatrixRMaj.class);

                // Set up equality constraint.
                for (int b = 0; b < settings.basisCount; b++)
                {
                    mQTQAugmented.set(b, settings.basisCount, 1.0);
                    mQTQAugmented.set(settings.basisCount, b, 1.0);
                }

                return mQTQAugmented;
            })
            .toArray(SimpleMatrix[]::new);

        weightsQTrAugmented = IntStream.range(0, settings.width * settings.height)
            .mapToObj(p ->
            {
                SimpleMatrix mQTrAugmented = new SimpleMatrix(settings.basisCount + 1, 1, DMatrixRMaj.class);
                mQTrAugmented.set(settings.basisCount, 1.0); // Set up equality constraint
                return mQTrAugmented;
            })
            .toArray(SimpleMatrix[]::new);
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
    {
        // Get GPU resources and disable back face culling since we're rendering in texture space
        IBRResources<ContextType> resources = renderable.getResources();
        resources.context.getState().disableBackFaceCulling();

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

        try
        (
            // Shader program to extract reflectance and other geometric info
            Program<ContextType> reflectanceProgram = createReflectanceProgram(resources);

            // Compare fitted models against actual photographs
            Program<ContextType> errorCalcProgram = createErrorCalcProgram(resources, settings.basisCount);

            // Rectangle vertex buffer
            VertexBuffer<ContextType> rect = resources.context.createRectangle();

            // Framebuffer used for extracting reflectance and related geometric data
            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject();

            // Framebuffer for calculating error and reconstructing 3D renderings of the object
            FramebufferObject<ContextType> tempFramebuffer =
                resources.context.buildFramebufferObject(imageWidth, imageHeight)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addDepthAttachment()
                    .createFramebufferObject();

            // Resources specific to this technique:

            // For K-means clustering
            Program<ContextType> averageProgram = createAverageProgram(resources);

            // Normal estimation program
            Program<ContextType> normalEstimationProgram = createNormalEstimationProgram(resources);

            // Fit specular parameters from weighted basis functions
            Program<ContextType> specularRoughnessFitProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularRoughnessFit.frag"))
                .define("BASIS_COUNT", settings.basisCount)
                .define("MICROFACET_DISTRIBUTION_RESOLUTION", settings.microfacetDistributionResolution)
                .createProgram();

            // Framebuffers (double-buffered) for estimating normals on the GPU
            FramebufferObject<ContextType> normalFramebuffer1 = resources.context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F) // Damping factor while fitting
                .createFramebufferObject();
            FramebufferObject<ContextType> normalFramebuffer2 = resources.context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F) // Damping factor while fitting
                .createFramebufferObject();

            // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
            FramebufferObject<ContextType> specularTexFramebuffer = resources.context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
                .createFramebufferObject();

            // Textures calculated on CPU and passed to GPU (not framebuffers)
            SpecularFitResources<ContextType> specularFitResources = new SpecularFitResources<ContextType>(resources.context, settings)
        )
        {
            specularFitResources.weightMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None, TextureWrapMode.None);
            specularFitResources.basisMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None);

            Drawable<ContextType> averageDrawable = createDrawable(averageProgram, resources);

            initializeClusters(averageDrawable, framebuffer);

            normalFramebuffer1.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
            normalFramebuffer2.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            // Double buffering since we need the previous normal estimate to generate the next normal estimate.
            FramebufferObject<ContextType> frontNormalFramebuffer = normalFramebuffer1;
            FramebufferObject<ContextType> backNormalFramebuffer = normalFramebuffer2;

            Drawable<ContextType> reflectanceDrawable = createDrawable(reflectanceProgram, resources);
            reflectanceProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
            reflectanceProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));

            Drawable<ContextType> normalEstimationDrawable = createDrawable(normalEstimationProgram, resources);
            normalEstimationProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            normalEstimationProgram.setTexture("weightMaps", specularFitResources.weightMaps);
            normalEstimationProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            normalEstimationProgram.setUniformBuffer("DiffuseColors", specularFitResources.diffuseUniformBuffer);

            Drawable<ContextType> errorCalcDrawable = createDrawable(errorCalcProgram, resources);
            errorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
            errorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            errorCalcProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            errorCalcProgram.setTexture("weightMaps", specularFitResources.weightMaps);
            errorCalcProgram.setUniformBuffer("DiffuseColors", specularFitResources.diffuseUniformBuffer);
            errorCalcProgram.setUniform("errorGamma", 1.0f);

            Drawable<ContextType> specularRoughnessFitDrawable = resources.context.createDrawable(specularRoughnessFitProgram);
            specularRoughnessFitDrawable.addVertexBuffer("position", rect);
            specularRoughnessFitProgram.setUniform("gamma", (float)GAMMA);
            specularRoughnessFitProgram.setTexture("weightMaps", specularFitResources.weightMaps);
            specularRoughnessFitProgram.setTexture("weightMask", specularFitResources.weightMask);
            specularRoughnessFitProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            specularRoughnessFitProgram.setUniform("fittingGamma", 1.0f);

            // Set initial assumption for roughness when calculating masking/shadowing.
            specularTexFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f,1.0f);

            for (int i = 0; i < settings.basisCount; i++)
            {
                solution.setDiffuseAlbedo(i, DoubleVector3.ZERO);
            }

            int viewCount = resources.viewSet.getCameraPoseCount();

            double previousError;

            do
            {
                previousError = errorCalculator.getError();

                // Use the current front normal buffer for extracting reflectance information.
                reflectanceProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));

                reconstructBRDFs(reflectanceDrawable, framebuffer, viewCount);

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

                reconstructWeights(reflectanceDrawable, framebuffer, viewCount);

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

            finalize(settings, resources, rect, framebuffer, tempFramebuffer, specularRoughnessFitDrawable, specularTexFramebuffer,
                errorCalcDrawable, errorCalculator, solution, specularFitResources, frontNormalFramebuffer.getColorAttachmentTexture(0));
        }
        catch(FileNotFoundException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>> void finalize(
        SpecularFitSettings settings,
        IBRResources<ContextType> resources,
        VertexBuffer<ContextType> rect,
        FramebufferObject<ContextType> framebuffer,
        FramebufferObject<ContextType> imageReconstructionFramebuffer,

        // Specular fit
        Drawable<ContextType> specularRoughnessFitDrawable,
        FramebufferObject<ContextType> specularTexFramebuffer,

        // Error calculation
        Drawable<ContextType> errorCalcDrawable,
        ShaderBasedErrorCalculator errorCalculator,

        // Solution
        SpecularFitSolution solution,
        SpecularFitResources<ContextType> specularFitResources,
        Texture2D<ContextType> normalMap)
    {
        try (
            // Text file containing error information
            PrintStream rmseOut = new PrintStream(new File(settings.outputDirectory, "rmse.txt"));

            // Error calculation shader programs
            Program<ContextType> finalErrorCalcProgram = createFinalErrorCalcProgram(resources, settings.basisCount);
            Program<ContextType> ggxErrorCalcProgram = createGGXErrorCalcProgram(resources);

            // Final diffuse estimation program
            Program<ContextType> diffuseEstimationProgram = createDiffuseEstimationProgram(resources, settings);

            // Hole fill program
            Program<ContextType> diffuseHoleFillProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/holefill.frag"))
                .createProgram();

            // Draw basis functions as supplemental output
            Program<ContextType> basisImageProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/basisImage.frag"))
                .createProgram();

            // Reconstruct images as supplemental output
            Program<ContextType> imageReconstructionProgram = createImageReconstructionProgram(resources, settings.basisCount);
            Program<ContextType> fittedImageReconstructionProgram = createFittedImageReconstructionProgram(resources);

            // Framebuffer for filling holes
            // This framebuffer is used to double-buffer another primary framebuffer
            FramebufferObject<ContextType> diffuseHoleFillFramebuffer = resources.context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorFormat.RGBA32F)
//                    .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject();

            // Framebuffer for visualizing the basis functions
            FramebufferObject<ContextType> basisImageFramebuffer = resources.context.buildFramebufferObject(
                2 * settings.microfacetDistributionResolution + 1, 2 * settings.microfacetDistributionResolution + 1)
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject();
        )
        {
            // Print out RMSE from the penultimate iteration (to verify convergence)
            rmseOut.println("Previously calculated RMSE: " + errorCalculator.getError());

            // Calculate the final RMSE from the raw result
            specularFitResources.updateFromSolution(solution);
            errorCalculator.update(errorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE before hole fill: " + errorCalculator.getError());

            // Fill holes in the weight map
            fillHoles(settings, solution);

            // Save the weight map and preliminary diffuse result after filling holes
            saveWeightMaps(settings, solution);
            saveDiffuseMap(settings, solution);

            // Calculate RMSE after filling holes
            specularFitResources.updateFromSolution(solution);
            errorCalculator.update(errorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE after hole fill: " + errorCalculator.getError());

            // Calculate gamma-corrected RMSE
            errorCalcDrawable.program().setUniform("errorGamma", 2.2f);
            errorCalculator.update(errorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE after hole fill (gamma-corrected): " + errorCalculator.getError());

            // Save basis functions in both image and text format.
            Drawable<ContextType> basisImageDrawable = resources.context.createDrawable(basisImageProgram);
            basisImageDrawable.addVertexBuffer("position", rect);
            basisImageProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            saveBasisFunctions(settings, solution, basisImageDrawable, basisImageFramebuffer);

            // Diffuse fit
            Drawable<ContextType> diffuseFitDrawable = createDrawable(diffuseEstimationProgram, resources);
            diffuseEstimationProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            diffuseEstimationProgram.setTexture("weightMaps", specularFitResources.weightMaps);
            diffuseEstimationProgram.setTexture("weightMask", specularFitResources.weightMask);
            diffuseEstimationProgram.setTexture("normalEstimate", normalMap);
            diffuseEstimationProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            diffuseFitDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            Drawable<ContextType> diffuseHoleFillDrawable = resources.context.createDrawable(diffuseHoleFillProgram);
            diffuseHoleFillDrawable.addVertexBuffer("position", rect);
            FramebufferObject<ContextType> finalDiffuse = fillHolesShader(
                diffuseHoleFillDrawable, framebuffer, diffuseHoleFillFramebuffer, Math.max(settings.width, settings.height));

            try
            {
                finalDiffuse.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "diffuse.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            Drawable<ContextType> finalErrorCalcDrawable = createDrawable(finalErrorCalcProgram, resources);
            finalErrorCalcProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            finalErrorCalcProgram.setTexture("weightMaps", specularFitResources.weightMaps);
            finalErrorCalcProgram.setTexture("weightMask", specularFitResources.weightMask);
            finalErrorCalcProgram.setTexture("normalEstimate", normalMap);
            finalErrorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            finalErrorCalcProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));
            finalErrorCalcProgram.setUniform("errorGamma", 1.0f);

            errorCalculator.update(finalErrorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE after final diffuse estimate: " + errorCalculator.getError());

            finalErrorCalcProgram.setUniform("errorGamma", 2.2f);
            errorCalculator.update(finalErrorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE after final diffuse estimate (gamma-corrected): " + errorCalculator.getError());

            Drawable<ContextType> ggxErrorCalcDrawable = createDrawable(ggxErrorCalcProgram, resources);
            ggxErrorCalcProgram.setTexture("normalEstimate", normalMap);
            ggxErrorCalcProgram.setTexture("specularEstimate", specularTexFramebuffer.getColorAttachmentTexture(0));
            ggxErrorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            ggxErrorCalcProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));
            ggxErrorCalcProgram.setUniform("errorGamma", 1.0f);
            errorCalculator.update(ggxErrorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE for GGX fit: " + errorCalculator.getError());

            ggxErrorCalcProgram.setUniform("errorGamma", 2.2f);
            errorCalculator.update(ggxErrorCalcDrawable, imageReconstructionFramebuffer);
            rmseOut.println("RMSE for GGX fit (gamma-corrected): " + errorCalculator.getError());

            // Render and save images using more accurate basis function reconstruction.
            Drawable<ContextType> imageReconstructionDrawable = createDrawable(imageReconstructionProgram, resources);
            imageReconstructionProgram.setTexture("basisFunctions", specularFitResources.basisMaps);
            imageReconstructionProgram.setTexture("weightMaps", specularFitResources.weightMaps);
            imageReconstructionProgram.setTexture("weightMask", specularFitResources.weightMask);
            imageReconstructionProgram.setTexture("normalEstimate", normalMap);
            imageReconstructionProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            imageReconstructionProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));

            reconstructImages(settings, imageReconstructionDrawable, imageReconstructionFramebuffer, resources.viewSet, "reconstructions");

            // Fit specular textures after filling holes
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

            // Render and save images using parameterized fit.
            Drawable<ContextType> fittedImageReconstructionDrawable = createDrawable(fittedImageReconstructionProgram, resources);
            fittedImageReconstructionProgram.setTexture("normalEstimate", normalMap);
            fittedImageReconstructionProgram.setTexture("specularEstimate", specularTexFramebuffer.getColorAttachmentTexture(0));
            fittedImageReconstructionProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            fittedImageReconstructionProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));

            reconstructImages(settings, fittedImageReconstructionDrawable, imageReconstructionFramebuffer, resources.viewSet, "fitted");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private <ContextType extends Context<ContextType>>
    Program<ContextType> createAverageProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/average.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.additional.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.additional.getBoolean("shadowsEnabled"))
            .createProgram();

        program.setUniform("occlusionBias", settings.additional.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>>
        Program<ContextType> createReflectanceProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/extractReflectance.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.additional.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.additional.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .createProgram();

        program.setUniform("occlusionBias", settings.additional.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>>
        Program<ContextType> createNormalEstimationProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateNormals.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.additional.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.additional.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("USE_LEVENBERG_MARQUARDT", USE_LEVENBERG_MARQUARDT ? 1 : 0)
            .define("BASIS_COUNT", settings.basisCount)
            .createProgram();

        program.setUniform("occlusionBias", settings.additional.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(IBRResources<ContextType> resources, int basisCount) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/colorappearance/imgspace_multi_as_single.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/errorCalc.frag"))
            .define("VISIBILITY_TEST_ENABLED", 0)
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", basisCount)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createFinalErrorCalcProgram(IBRResources<ContextType> resources, int basisCount) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/colorappearance/imgspace_multi_as_single.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/finalErrorCalc.frag"))
            .define("VISIBILITY_TEST_ENABLED", 0)
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", basisCount)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createGGXErrorCalcProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/colorappearance/imgspace_multi_as_single.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/ggxErrorCalc.frag"))
            .define("VISIBILITY_TEST_ENABLED", 0)
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createDiffuseEstimationProgram(IBRResources<ContextType> resources, SpecularFitSettings settings) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateDiffuse.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.additional.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.additional.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", settings.basisCount)
            .createProgram();

        program.setUniform("occlusionBias", settings.additional.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
        Program<ContextType> createImageReconstructionProgram(IBRResources<ContextType> resources, int basisCount) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/reconstructImage.frag"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", basisCount)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createFittedImageReconstructionProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/renderFit.frag"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>> void initializeClusters(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        System.out.println("Clustering to initialize weights...");

        // Clear framebuffer
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        // Run shader program to fill framebuffer with per-pixel information.
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        if (DEBUG)
        {
            try
            {
                framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "average.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        float[] averages = framebuffer.readFloatingPointColorBufferRGBA(0);

        // k-means++ initialization
        Random random = new SecureRandom();

        // Randomly choose the first center.
        int firstCenterIndex;

        do
        {
            firstCenterIndex = random.nextInt(settings.width * settings.height);
        }
        while(averages[4 * firstCenterIndex + 3] < 1.0); // Make sure the center chosen is valid.

        DoubleVector3[] centers = new DoubleVector3[settings.basisCount];
        centers[0] = new DoubleVector3(averages[4 * firstCenterIndex], averages[4 * firstCenterIndex + 1], averages[4 * firstCenterIndex + 2]);

        // Populate a CDF for the purpose of randomly selecting from a weighted probability distribution.
        double[] cdf = new double[settings.width * settings.height + 1];

        for (int b = 1; b < settings.basisCount; b++)
        {
            cdf[0] = 0.0;

            for (int p = 0; p < settings.width * settings.height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    double minDistance = Double.MAX_VALUE;

                    for (int b2 = 0; b2 < b; b2++)
                    {
                        minDistance = Math.min(minDistance, centers[b2].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2])));
                    }

                    cdf[p + 1] = cdf[p] + minDistance * minDistance;
                }
            }

            double x = random.nextDouble() * cdf[settings.width * settings.height - 1];

            //noinspection FloatingPointEquality
            if (x >= cdf[settings.width * settings.height - 1]) // It's possible but extremely unlikely that floating-point rounding would cause this to happen.
            {
                // In that extremely rare case, just set x to 0.0.
                x = 0.0;
            }

            // binarySearch returns index of a match if its found, or -insertionPoint - 1 if not (the more likely scenario).
            // insertionPoint is defined to be the index of the first element greater than the number searched for (the randomly generated value).
            int index = Arrays.binarySearch(cdf, x);

            if (index < 0) // The vast majority of the time this condition will be true, since floating-point matches are unlikely.
            {
                // We actually want insertionPoint - 1 since the CDF is offset by one from the actual array of colors.
                // i.e. if the random value falls between indices 3 and 4 in the CDF, the differential between those two indices is due to the color
                // at index 3, so we want to use 3 as are index.
                index = -index - 2;
            }

            assert index < cdf.length - 1;

            // If the index was actually positive to begin with, that's probably fine; just make sure that it's a valid location.
            // It's also possible in theory for the index to be zero if the random number generator produced 0.0.
            while (index < 0 || averages[4 * index + 3] == 0.0)
            {
                // Search forward until a valid index is found.
                index++;

                // We shouldn't ever fail to find an index since x should have been less than the final (un-normalized) CDF total.
                // This means that there has to be some place where the CDF went up, corresponding to a valid index.
                assert index < cdf.length - 1;
            }

            // We've found a new center.
            centers[b] = new DoubleVector3(averages[4 * index], averages[4 * index + 1], averages[4 * index + 2]);
        }

        System.out.println("Initial centers:");
        for (int b = 0; b < settings.basisCount; b++)
        {
            System.out.println(centers[b]);
        }

        // Initialization is done; now it's time to iterate.
        boolean changed;
        do
        {
            // Initialize sums to zero.
            DoubleVector4[] sums = IntStream.range(0, settings.basisCount).mapToObj(i -> DoubleVector4.ZERO_DIRECTION).toArray(DoubleVector4[]::new);

            for (int p = 0; p < settings.width * settings.height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < settings.basisCount; b++)
                    {
                        double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            bMin = b;
                        }
                    }

                    sums[bMin] = sums[bMin].plus(new DoubleVector4(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2], 1.0f));
                }
            }

            changed = false;
            for (int b = 0; b < settings.basisCount; b++)
            {
                if (sums[b].w > 0.0)
                {
                    DoubleVector3 newCenter = sums[b].getXYZ().dividedBy(sums[b].w);
                    changed = changed || newCenter.distance(centers[b]) > K_MEANS_TOLERANCE;
                    centers[b] = newCenter;
                }
            }
        }
        while (changed);

        for (int p = 0; p < settings.width * settings.height; p++)
        {
            // Initialize weights to zero.
            solution.getWeights(p).zero();

            if (averages[4 * p + 3] > 0.0)
            {
                int bMin = -1;

                double minDistance = Double.MAX_VALUE;

                for (int b = 0; b < settings.basisCount; b++)
                {
                    double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        bMin = b;
                    }
                }

                // Set weight to one for the cluster that each pixel belongs to.
                solution.getWeights(p).set(bMin, 1.0);
            }
        }

        System.out.println("Refined centers:");
        for (int b = 0; b < settings.basisCount; b++)
        {
            System.out.println(centers[b]);
        }

        if (DEBUG)
        {
            BufferedImage weightImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[settings.width * settings.height];
            for (int p = 0; p < settings.width * settings.height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < settings.basisCount; b++)
                    {
                        double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            bMin = b;
                        }
                    }

                    // Flip vertically
                    int weightDataIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);

                    switch(bMin)
                    {
                        case 0: weightDataPacked[weightDataIndex] = Color.RED.getRGB(); break;
                        case 1: weightDataPacked[weightDataIndex] = Color.GREEN.getRGB(); break;
                        case 2: weightDataPacked[weightDataIndex] = Color.BLUE.getRGB(); break;
                        case 3: weightDataPacked[weightDataIndex] = Color.YELLOW.getRGB(); break;
                        case 4: weightDataPacked[weightDataIndex] = Color.CYAN.getRGB(); break;
                        case 5: weightDataPacked[weightDataIndex] = Color.MAGENTA.getRGB(); break;
                        case 6: weightDataPacked[weightDataIndex] = Color.WHITE.getRGB(); break;
                        case 7: weightDataPacked[weightDataIndex] = Color.GRAY.getRGB(); break;
                        default: weightDataPacked[weightDataIndex] = Color.BLACK.getRGB(); break;
                    }
                }
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(settings.outputDirectory, "k-means.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private <ContextType extends Context<ContextType>> void reconstructBRDFs(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        System.out.println("Building reflectance fitting matrix...");
        buildReflectanceMatrix(drawable, framebuffer, viewCount);
        System.out.println("Finished building matrix; solving now...");
        double medianATyRed = IntStream.range(0, brdfATyRed.getNumElements()).mapToDouble(brdfATyRed::get)
            .sorted().skip(brdfATyRed.getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        double medianATyGreen = IntStream.range(0, brdfATyGreen.getNumElements()).mapToDouble(brdfATyGreen::get)
            .sorted().skip(brdfATyGreen.getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        double medianATyBlue = IntStream.range(0, brdfATyBlue.getNumElements()).mapToDouble(brdfATyBlue::get)
            .sorted().skip(brdfATyBlue.getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        SimpleMatrix brdfSolutionRed = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyRed, NNLS_TOLERANCE_SCALE * medianATyRed);
        SimpleMatrix brdfSolutionGreen = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyGreen, NNLS_TOLERANCE_SCALE * medianATyGreen);
        SimpleMatrix brdfSolutionBlue = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyBlue, NNLS_TOLERANCE_SCALE * medianATyBlue);
        System.out.println("DONE!");

        for (int b = 0; b < settings.basisCount; b++)
        {
            int bCopy = b;

            // Only update if the BRDF has non-zero elements.
            if (IntStream.range(0, settings.microfacetDistributionResolution + 1).anyMatch(
                i -> brdfSolutionRed.get(bCopy + settings.basisCount * i) > 0
                    || brdfSolutionGreen.get(bCopy + settings.basisCount * i) > 0
                    || brdfSolutionBlue.get(bCopy + settings.basisCount * i) > 0))
            {
                DoubleVector3 baseColor = new DoubleVector3(brdfSolutionRed.get(b), brdfSolutionGreen.get(b), brdfSolutionBlue.get(b));
                solution.setDiffuseAlbedo(b, baseColor.times(1.0 - METALLICITY));

                solution.getSpecularRed().set(settings.microfacetDistributionResolution, b, baseColor.x * METALLICITY);
                solution.getSpecularGreen().set(settings.microfacetDistributionResolution, b, baseColor.y * METALLICITY);
                solution.getSpecularBlue().set(settings.microfacetDistributionResolution, b, baseColor.z * METALLICITY);

                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    // f[m] = f[m+1] + estimated difference (located at index m + 1 due to diffuse component at index 0).
                    solution.getSpecularRed().set(m, b, solution.getSpecularRed().get(m + 1, b) + brdfSolutionRed.get((m + 1) * settings.basisCount + b));
                    solution.getSpecularGreen().set(m, b, solution.getSpecularGreen().get(m + 1, b) + brdfSolutionGreen.get((m + 1) * settings.basisCount + b));
                    solution.getSpecularBlue().set(m, b, solution.getSpecularBlue().get(m + 1, b) + brdfSolutionBlue.get((m + 1) * settings.basisCount + b));
                }
            }
        }

        if (DEBUG)
        {
            System.out.println();

            for (int b = 0; b < settings.basisCount; b++)
            {
                DoubleVector3 diffuseColor = new DoubleVector3(
                    brdfSolutionRed.get(b),
                    brdfSolutionGreen.get(b),
                    brdfSolutionBlue.get(b));
                System.out.println("Diffuse #" + b + ": " + diffuseColor);
            }

            System.out.println("Basis BRDFs:");

            for (int b = 0; b < settings.basisCount; b++)
            {
                System.out.print("Red#" + b);
                double redTotal = 0.0;
                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    redTotal += brdfSolutionRed.get((m + 1) * settings.basisCount + b);
                    System.out.print(redTotal);
                }

                System.out.println();

                System.out.print("Green#" + b);
                double greenTotal = 0.0;
                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    greenTotal += brdfSolutionGreen.get((m + 1) * settings.basisCount + b);
                    System.out.print(greenTotal);
                }
                System.out.println();

                System.out.print("Blue#" + b);
                double blueTotal = 0.0;
                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    blueTotal += brdfSolutionBlue.get((m + 1) * settings.basisCount + b);
                    System.out.print(blueTotal);
                }
                System.out.println();
            }

            System.out.println();
        }
    }

    private <ContextType extends Context<ContextType>> void buildReflectanceMatrix(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        // Reinitialize matrices to zero.
        brdfATA.zero();
        brdfATyRed.zero();
        brdfATyGreen.zero();
        brdfATyBlue.zero();

        for (int k = 0; k < viewCount; k++)
        {
            synchronized (threadsRunningLock)
            {
                // Make sure that we don't have too many threads running.
                // Wait until a thread finishes if we're at the max.
                while (threadsRunning >= MAX_RUNNING_THREADS)
                {
                    try
                    {
                        threadsRunningLock.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            // Clear framebuffer
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.program().setUniform("viewIndex", k);
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Copy framebuffer from GPU to main memory.
            float[] colorAndVisibility = framebuffer.readFloatingPointColorBufferRGBA(0);
            float[] halfwayAndGeom = framebuffer.readFloatingPointColorBufferRGBA(1);

            synchronized (threadsRunningLock)
            {
                threadsRunning++;
            }

            int kCopy = k;

            Thread contributionThread = new Thread(() ->
            {
                try
                {
                    // Create scratch space for the thread handling this view.
                    SimpleMatrix contributionATA = new SimpleMatrix(brdfMatrixSize, brdfMatrixSize, DMatrixRMaj.class);
                    SimpleMatrix contributionATyRed = new SimpleMatrix(brdfMatrixSize, 1, DMatrixRMaj.class);
                    SimpleMatrix contributionATyGreen = new SimpleMatrix(brdfMatrixSize, 1, DMatrixRMaj.class);
                    SimpleMatrix contributionATyBlue = new SimpleMatrix(brdfMatrixSize, 1, DMatrixRMaj.class);

                    // Get the contributions from the current view.
                    new ReflectanceMatrixBuilder(colorAndVisibility, halfwayAndGeom, solution, contributionATA,
                        contributionATyRed, contributionATyGreen, contributionATyBlue, METALLICITY).execute();

                    // Add the contribution into the main matrix and vectors.
                    synchronized (brdfATA)
                    {
                        CommonOps_DDRM.addEquals(brdfATA.getMatrix(), contributionATA.getMatrix());
                    }

                    synchronized (brdfATyRed)
                    {
                        CommonOps_DDRM.addEquals(brdfATyRed.getMatrix(), contributionATyRed.getMatrix());
                    }

                    synchronized (brdfATyGreen)
                    {
                        CommonOps_DDRM.addEquals(brdfATyGreen.getMatrix(), contributionATyGreen.getMatrix());
                    }

                    synchronized (brdfATyBlue)
                    {
                        CommonOps_DDRM.addEquals(brdfATyBlue.getMatrix(), contributionATyBlue.getMatrix());
                    }
                }
                finally
                {
                    synchronized (threadsRunningLock)
                    {
                        threadsRunning--;
                        threadsRunningLock.notifyAll();
                    }

                    System.out.println("Finished view " + kCopy + '.');
                }
            });

            contributionThread.start();
        }

        // Wait for all the threads to finish.
        synchronized (threadsRunningLock)
        {
            while (threadsRunning > 0)
            {
                try
                {
                    threadsRunningLock.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        if (DEBUG)
        {
            System.out.println();

            for (int b = 0; b < settings.basisCount; b++)
            {
                System.out.print("RHS, red for BRDF #" + b + ": ");

                System.out.print(brdfATyRed.get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(brdfATyRed.get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.print("RHS, green for BRDF #" + b + ": ");

                System.out.print(brdfATyGreen.get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(brdfATyGreen.get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.print("RHS, blue for BRDF #" + b + ": ");

                System.out.print(brdfATyBlue.get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(brdfATyBlue.get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.println();
            }
        }
    }

    private <ContextType extends Context<ContextType>> void reconstructWeights(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        System.out.println("Building weight fitting matrices...");

        // Setup all the matrices for fitting weights (one per texel)
        buildWeightMatrices(drawable, framebuffer, viewCount);

        System.out.println("Finished building matrices; solving now...");

        for (int p = 0; p < settings.width * settings.height; p++)
        {
            if (solution.areWeightsValid(p))
            {
                double median = IntStream.range(0, weightsQTrAugmented[p].getNumElements()).mapToDouble(weightsQTrAugmented[p]::get)
                    .sorted().skip(weightsQTrAugmented[p].getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
                solution.setWeights(p, NonNegativeLeastSquares.solvePremultipliedWithEqualityConstraints(
                    weightsQTQAugmented[p], weightsQTrAugmented[p], median * NNLS_TOLERANCE_SCALE, 1));
            }
        }

        System.out.println("DONE!");

        if (DEBUG)
        {
            // write out weight textures for debugging
            saveWeightMaps(settings, solution);

            // write out diffuse texture for debugging
            saveDiffuseMap(settings, solution);
        }
    }

    private <ContextType extends Context<ContextType>> void buildWeightMatrices(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        // Initially assume that all texels are invalid.
        solution.invalidateWeights();

        for (int k = 0; k < viewCount; k++)
        {
            // Clear framebuffer
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.program().setUniform("viewIndex", k);
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Copy framebuffer from GPU to main memory.
            float[] colorAndVisibility = framebuffer.readFloatingPointColorBufferRGBA(0);
            float[] halfwayAndGeomAndWeights = framebuffer.readFloatingPointColorBufferRGBA(1);

            // Update matrix for each pixel.
            IntStream.range(0, settings.width * settings.height).parallel().forEach(p ->
            {
                // Skip samples that aren't visible or are otherwise invalid.
                if (colorAndVisibility[4 * p + 3] > 0)
                {
                    // Any time we have a visible, valid sample, mark that the corresponding texel is valid.
                    solution.setWeightsValidity(p, true);

                    // Precalculate frequently used values.

                    // For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
                    // so we don't want to multiply by n dot l when attempting to reproduce that version.
                    double nDotLSquared = ORIGINAL_NAM_METHOD ? 1.0 : halfwayAndGeomAndWeights[4 * p + 3] * halfwayAndGeomAndWeights[4 * p + 3];

                    double weightSquared = halfwayAndGeomAndWeights[4 * p + 2] * halfwayAndGeomAndWeights[4 * p + 2];
                    double geomFactor = halfwayAndGeomAndWeights[4 * p + 1];
                    double mExact = halfwayAndGeomAndWeights[4 * p] * settings.microfacetDistributionResolution;
                    int m1 = (int)Math.floor(mExact);
                    int m2 = m1 + 1;
                    double t = mExact - m1;
                    DoubleVector3 fActual = new DoubleVector3(colorAndVisibility[4 * p], colorAndVisibility[4 * p + 1], colorAndVisibility[4 * p + 2]);

                    for (int b1 = 0; b1 < settings.basisCount; b1++)
                    {
                        // Evaluate the first basis BRDF.
                        DoubleVector3 f1 = solution.getDiffuseAlbedo(b1).dividedBy(PI);

                        if (m1 < settings.microfacetDistributionResolution)
                        {
                            f1 = f1.plus(new DoubleVector3(solution.getSpecularRed().get(m1, b1), solution.getSpecularGreen().get(m1, b1), solution.getSpecularBlue().get(m1, b1))
                                .times(1.0 - t)
                                .plus(new DoubleVector3(solution.getSpecularRed().get(m2, b1), solution.getSpecularGreen().get(m2, b1), solution.getSpecularBlue().get(m2, b1))
                                    .times(t))
                                .times(geomFactor));
                        }
                        else if (METALLICITY > 0.0f)
                        {
                            f1 = f1.plus(new DoubleVector3(
                                    solution.getSpecularRed().get(settings.microfacetDistributionResolution, b1),
                                    solution.getSpecularGreen().get(settings.microfacetDistributionResolution, b1),
                                    solution.getSpecularBlue().get(settings.microfacetDistributionResolution, b1))
                                .times(geomFactor));
                        }

                        // Store the weighted product of the basis BRDF and the actual BRDF in the vector.
                        weightsQTrAugmented[p].set(b1, weightsQTrAugmented[p].get(b1) + weightSquared * nDotLSquared * f1.dot(fActual));

                        for (int b2 = 0; b2 < settings.basisCount; b2++)
                        {
                            // Evaluate the second basis BRDF.
                            DoubleVector3 f2 = solution.getDiffuseAlbedo(b2).dividedBy(PI);

                            if (m1 < settings.microfacetDistributionResolution)
                            {
                                f2 = f2.plus(new DoubleVector3(solution.getSpecularRed().get(m1, b2), solution.getSpecularGreen().get(m1, b2), solution.getSpecularBlue().get(m1, b2))
                                    .times(1.0 - t)
                                    .plus(new DoubleVector3(solution.getSpecularRed().get(m2, b2), solution.getSpecularGreen().get(m2, b2), solution.getSpecularBlue().get(m2, b2))
                                        .times(t))
                                    .times(geomFactor));
                            }
                            else if (METALLICITY > 0.0f)
                            {
                                f2 = f2.plus(new DoubleVector3(
                                        solution.getSpecularRed().get(settings.microfacetDistributionResolution, b2),
                                        solution.getSpecularGreen().get(settings.microfacetDistributionResolution, b2),
                                        solution.getSpecularBlue().get(settings.microfacetDistributionResolution, b2))
                                    .times(geomFactor));
                            }

                            // Store the weighted product of the two BRDFs in the matrix.
                            weightsQTQAugmented[p].set(b1, b2, weightsQTQAugmented[p].get(b1, b2) + weightSquared * nDotLSquared * f1.dot(f2));
                        }
                    }
                }
            });

            System.out.println("Finished view " + k + '.');
        }
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

    private static <ContextType extends Context<ContextType>>
    FramebufferObject<ContextType> fillHolesShader(
        Drawable<ContextType> holeFillDrawable, FramebufferObject<ContextType> initFrontFramebuffer,
        FramebufferObject<ContextType> initBackFramebuffer, int iterations)
    {
        FramebufferObject<ContextType> frontFramebuffer = initFrontFramebuffer;
        FramebufferObject<ContextType> backFramebuffer = initBackFramebuffer;

        for (int i = 0; i < iterations; i++)
        {
            holeFillDrawable.program().setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
            holeFillDrawable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);

            FramebufferObject<ContextType> tmp = frontFramebuffer;
            frontFramebuffer = backFramebuffer;
            backFramebuffer = tmp;
        }

        return frontFramebuffer;
    }

    private static void fillHoles(SpecularFitSettings settings, SpecularFitSolution solution)
    {
        // Fill holes
        // TODO Quick hack; should be replaced with something more robust.
        System.out.println("Filling holes...");

        int texelCount = settings.width * settings.height;

        for (int i = 0; i < Math.max(settings.width, settings.height); i++)
        {
            Collection<Integer> filledPositions = new HashSet<>(256);
            for (int p = 0; p < texelCount; p++)
            {
                if (!solution.areWeightsValid(p))
                {
                    int left = (texelCount + p - 1) % texelCount;
                    int right = (p + 1) % texelCount;
                    int up = (texelCount + p - settings.width) % texelCount;
                    int down = (p + settings.width) % texelCount;

                    int count = 0;

                    for (int b = 0; b < settings.basisCount; b++)
                    {
                        count = 0;
                        double sum = 0.0;

                        if (solution.areWeightsValid(left))
                        {
                            sum += solution.getWeights(left).get(b);
                            count++;
                        }

                        if (solution.areWeightsValid(right))
                        {
                            sum += solution.getWeights(right).get(b);
                            count++;
                        }

                        if (solution.areWeightsValid(up))
                        {
                            sum += solution.getWeights(up).get(b);
                            count++;
                        }

                        if (solution.areWeightsValid(down))
                        {
                            sum += solution.getWeights(down).get(b);
                            count++;
                        }

                        if (sum > 0.0)
                        {
                            solution.getWeights(p).set(b, sum / count);
                        }
                    }

                    if (count > 0)
                    {
                        filledPositions.add(p);
                    }
                }
            }

            for (int p : filledPositions)
            {
                solution.setWeightsValidity(p, true);
            }
        }

        System.out.println("DONE!");
    }

    private static <ContextType extends Context<ContextType>> void saveBasisFunctions(
        SpecularFitSettings settings, SpecularFitSolution solution,
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(settings.outputDirectory, "basisFunctions.csv")))
        {
            for (int b = 0; b < settings.basisCount; b++)
            {
                out.print("Red#" + b);
                for (int m = 0; m <= settings.microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(solution.getSpecularRed().get(m, b));
                }
                out.println();

                out.print("Green#" + b);
                for (int m = 0; m <= settings.microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(solution.getSpecularGreen().get(m, b));
                }
                out.println();

                out.print("Blue#" + b);
                for (int m = 0; m <= settings.microfacetDistributionResolution; m++)
                {
                    out.print(", ");
                    out.print(solution.getSpecularBlue().get(m, b));
                }
                out.println();
            }

            out.println();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // Image format
        try
        {
            for (int i = 0; i < settings.basisCount; i++)
            {
                drawable.program().setUniform("basisIndex", i);
                drawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, String.format("basis_%02d.png", i)));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void saveWeightMaps(SpecularFitSettings settings, SpecularFitSolution solution)
    {
        for (int b = 0; b < settings.basisCount; b++)
        {
            BufferedImage weightImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[settings.width * settings.height];

            for (int p = 0; p < settings.width * settings.height; p++)
            {
                float weight = (float)solution.getWeights(p).get(b);

                // Flip vertically
                int dataBufferIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(settings.outputDirectory, String.format("weights%02d.png", b)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void saveDiffuseMap(SpecularFitSettings settings, SpecularFitSolution solution)
    {
        BufferedImage diffuseImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
        int[] diffuseDataPacked = new int[settings.width * settings.height];
        for (int p = 0; p < settings.width * settings.height; p++)
        {
            DoubleVector4 diffuseSum = DoubleVector4.ZERO_DIRECTION;

            for (int b = 0; b < settings.basisCount; b++)
            {
                diffuseSum = diffuseSum.plus(solution.getDiffuseAlbedo(b).asVector4(1.0)
                    .times(solution.getWeights(p).get(b)));
            }

            if (diffuseSum.w > 0)
            {
                DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / GAMMA)));

                // Flip vertically
                int dataBufferIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);
                diffuseDataPacked[dataBufferIndex] = new Color((float) diffuseAvgGamma.x, (float) diffuseAvgGamma.y, (float) diffuseAvgGamma.z).getRGB();
            }
        }

        diffuseImg.setRGB(0, 0, diffuseImg.getWidth(), diffuseImg.getHeight(), diffuseDataPacked, 0, diffuseImg.getWidth());

        try
        {
            ImageIO.write(diffuseImg, "PNG", new File(settings.outputDirectory, "diffuse_frombasis.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private <ContextType extends Context<ContextType>> void saveNormalMap(Framebuffer<ContextType> framebuffer)
    {
        saveNormalMap(framebuffer, "normal.png");
    }

    private <ContextType extends Context<ContextType>> void saveNormalMap(Framebuffer<ContextType> framebuffer, String filename)
    {
        try
        {
            framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>> void reconstructImages(
        TextureFitSettings settings, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, ViewSet viewSet, String folderName)
    {
        new File(settings.outputDirectory, folderName).mkdir();

        for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
        {
            drawable.program().setUniform("model_view", viewSet.getCameraPose(k));
            drawable.program().setUniform("projection",
                viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(k)).getProjectionMatrix(
                    viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
            drawable.program().setUniform("viewIndex", k);

            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
            framebuffer.clearDepthBuffer();
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            String filename = String.format("%04d.png", k);

            try
            {
                framebuffer.saveColorBufferToFile(0, "PNG",
                    new File(new File(settings.outputDirectory, folderName), filename));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
