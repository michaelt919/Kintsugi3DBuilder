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
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.NonNegativeLeastSquares;

import static java.lang.Math.PI;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularFitRequest implements IBRRequest
{
    static final boolean ORIGINAL_NAM_METHOD = false;
    static final boolean USE_LEVENBERG_MARQUARDT = !ORIGINAL_NAM_METHOD;

    static final int BASIS_COUNT = 8;
    static final int MICROFACET_DISTRIBUTION_RESOLUTION = 90;

    private static final int BRDF_MATRIX_SIZE = BASIS_COUNT * (MICROFACET_DISTRIBUTION_RESOLUTION + 1);
    private static final double K_MEANS_TOLERANCE = 0.0001;
    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;
    private static final double CONVERGENCE_TOLERANCE = 0.0001;
    private static final double GAMMA = 2.2;

    private static final boolean NORMAL_REFINEMENT = true;

    private static final boolean DEBUG = true;
    private static final int MAX_RUNNING_THREADS = 5;

    private final int width;
    private final int height;
    private final File outputDirectory;
    private final ReadonlySettingsModel settingsModel;
    private static final double METALLICITY = 0.0f; // Implemented and minimally tested but doesn't seem to make much difference.

    private double error = Double.POSITIVE_INFINITY;
//    private double weightedError = Double.POSITIVE_INFINITY;

    private final SimpleMatrix brdfATA = new SimpleMatrix(BRDF_MATRIX_SIZE, BRDF_MATRIX_SIZE, DMatrixRMaj.class);
    private final SimpleMatrix brdfATyRed = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
    private final SimpleMatrix brdfATyGreen = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
    private final SimpleMatrix brdfATyBlue = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
    private final DoubleVector3[] diffuseAlbedos = new DoubleVector3[BASIS_COUNT];
    private final SimpleMatrix specularRed = new SimpleMatrix(MICROFACET_DISTRIBUTION_RESOLUTION + 1, BASIS_COUNT, DMatrixRMaj.class);
    private final SimpleMatrix specularGreen = new SimpleMatrix(MICROFACET_DISTRIBUTION_RESOLUTION + 1, BASIS_COUNT, DMatrixRMaj.class);
    private final SimpleMatrix specularBlue = new SimpleMatrix(MICROFACET_DISTRIBUTION_RESOLUTION + 1, BASIS_COUNT, DMatrixRMaj.class);

    private final SimpleMatrix[] weightsQTQAugmented;
    private final SimpleMatrix[] weightsQTrAugmented;
    private final boolean[] weightsValidity;
    private final SimpleMatrix[] weightSolutions;

    private final Object threadsRunningLock = new Object();
    private int threadsRunning = 0;

    public SpecularFitRequest(int width, int height, File outputDirectory, ReadonlySettingsModel settingsModel)
    {
        this.width = width;
        this.height = height;
        this.outputDirectory = outputDirectory;
        this.settingsModel = settingsModel;

        weightSolutions = IntStream.range(0, width * height)
            .mapToObj(p -> new SimpleMatrix(BASIS_COUNT + 1, 1, DMatrixRMaj.class))
            .toArray(SimpleMatrix[]::new);

        weightsQTQAugmented = IntStream.range(0, width * height)
            .mapToObj(p ->
            {
                SimpleMatrix mQTQAugmented = new SimpleMatrix(BASIS_COUNT + 1, BASIS_COUNT + 1, DMatrixRMaj.class);

                // Set up equality constraint.
                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    mQTQAugmented.set(b, BASIS_COUNT, 1.0);
                    mQTQAugmented.set(BASIS_COUNT, b, 1.0);
                }

                return mQTQAugmented;
            })
            .toArray(SimpleMatrix[]::new);

        weightsQTrAugmented = IntStream.range(0, width * height)
            .mapToObj(p ->
            {
                SimpleMatrix mQTrAugmented = new SimpleMatrix(BASIS_COUNT + 1, 1, DMatrixRMaj.class);
                mQTrAugmented.set(BASIS_COUNT, 1.0); // Set up equality constraint
                return mQTrAugmented;
            })
            .toArray(SimpleMatrix[]::new);

        weightsValidity = new boolean[width * height];
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
    {
        IBRResources<ContextType> resources = renderable.getResources();
        resources.context.getState().disableBackFaceCulling();

        Projection defaultProj = resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(
            resources.viewSet.getPrimaryViewIndex()));

        int imageWidth;
        int imageHeight;

        if (defaultProj.getAspectRatio() < 1.0)
        {
            imageWidth = width;
            imageHeight = Math.round(imageWidth / defaultProj.getAspectRatio());
        }
        else
        {
            imageHeight = height;
            imageWidth = Math.round(imageHeight * defaultProj.getAspectRatio());
        }

        try
        (
            Program<ContextType> averageProgram = createAverageProgram(resources);
            Program<ContextType> reflectanceProgram = createReflectanceProgram(resources);
            Program<ContextType> normalEstimationProgram = createNormalEstimationProgram(resources);
            Program<ContextType> diffuseEstimationProgram = createDiffuseEstimationProgram(resources);
            Program<ContextType> diffuseHoleFillProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/holefill.frag"))
                .createProgram();
            Program<ContextType> errorCalcProgram = createErrorCalcProgram(resources);
            Program<ContextType> finalErrorCalcProgram = createFinalErrorCalcProgram(resources);
            Program<ContextType> ggxErrorCalcProgram = createGGXErrorCalcProgram(resources);
            Program<ContextType> imageReconstructionProgram = createImageReconstructionProgram(resources);
            Program<ContextType> fittedImageReconstructionProgram = createFittedImageReconstructionProgram(resources);
            Program<ContextType> basisImageProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/basisImage.frag"))
                .createProgram();
            Program<ContextType> specularFitProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularFit.frag"))
                .define("BASIS_COUNT", BASIS_COUNT)
                .define("MICROFACET_DISTRIBUTION_RESOLUTION", MICROFACET_DISTRIBUTION_RESOLUTION)
                .createProgram();
            VertexBuffer<ContextType> rect = resources.context.createRectangle();
            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject();
            FramebufferObject<ContextType> normalFramebuffer1 = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F) // Damping factor while fitting
                .createFramebufferObject();
            FramebufferObject<ContextType> normalFramebuffer2 = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F) // Damping factor while fitting
                .createFramebufferObject();
            FramebufferObject<ContextType> imageReconstructionFramebuffer =
                resources.context.buildFramebufferObject(imageWidth, imageHeight)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addDepthAttachment()
                    .createFramebufferObject();
            FramebufferObject<ContextType> basisImageFramebuffer = resources.context.buildFramebufferObject(
                    2 * MICROFACET_DISTRIBUTION_RESOLUTION + 1, 2 * MICROFACET_DISTRIBUTION_RESOLUTION + 1)
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject();
            FramebufferObject<ContextType> specularTexFramebuffer = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
                .createFramebufferObject();
            FramebufferObject<ContextType> diffuseHoleFillFramebuffer = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject();
            Texture3D<ContextType> weightMaps = resources.context.getTextureFactory().build2DColorTextureArray(width, height, BASIS_COUNT)
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture();
            Texture2D<ContextType> weightMask = resources.context.getTextureFactory().build2DColorTexture(width, height)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture();
            Texture2D<ContextType> basisMaps = resources.context.getTextureFactory().build1DColorTextureArray(
                    MICROFACET_DISTRIBUTION_RESOLUTION + 1, BASIS_COUNT)
                .setInternalFormat(ColorFormat.RGB32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture();
            UniformBuffer<ContextType> diffuseUniformBuffer = resources.context.createUniformBuffer()
        )
        {
            weightMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None, TextureWrapMode.None);
            basisMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None);

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
            normalEstimationProgram.setTexture("basisFunctions", basisMaps);
            normalEstimationProgram.setTexture("weightMaps", weightMaps);
            normalEstimationProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            normalEstimationProgram.setUniformBuffer("DiffuseColors", diffuseUniformBuffer);

            Drawable<ContextType> errorCalcDrawable = createDrawable(errorCalcProgram, resources);
            errorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
            errorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
            errorCalcProgram.setTexture("basisFunctions", basisMaps);
            errorCalcProgram.setTexture("weightMaps", weightMaps);
            errorCalcProgram.setUniformBuffer("DiffuseColors", diffuseUniformBuffer);
            errorCalcProgram.setUniform("errorGamma", 1.0f);

            Drawable<ContextType> basisImageDrawable = resources.context.createDrawable(basisImageProgram);
            basisImageDrawable.addVertexBuffer("position", rect);
            basisImageProgram.setTexture("basisFunctions", basisMaps);

            Drawable<ContextType> specularFitDrawable = resources.context.createDrawable(specularFitProgram);
            specularFitDrawable.addVertexBuffer("position", rect);
            specularFitProgram.setUniform("gamma", (float)GAMMA);
            specularFitProgram.setTexture("weightMaps", weightMaps);
            specularFitProgram.setTexture("weightMask", weightMask);
            specularFitProgram.setTexture("basisFunctions", basisMaps);
            specularFitProgram.setUniform("fittingGamma", 1.0f);

            // Set initial assumption for roughness when calculating masking/shadowing.
            specularTexFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f,1.0f);

            Arrays.fill(diffuseAlbedos, DoubleVector3.ZERO);

            int viewCount = resources.viewSet.getCameraPoseCount();

            double previousError;

            do
            {
                previousError = error;

                // Use the current front normal buffer for extracting reflectance information.
                reflectanceProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));

                reconstructBRDFs(reflectanceDrawable, framebuffer, viewCount);

                // Calculate the error for debugging.
                if (DEBUG)
                {
                    double previousErrorLocal = error;
                    updateError(errorCalcDrawable, imageReconstructionFramebuffer);

                    System.out.println("--------------------------------------------------");
                    System.out.println("Error: " + error);
                    System.out.println("(Previous error: " + previousErrorLocal + ')');
                    System.out.println("--------------------------------------------------");
                    System.out.println();
                }

                reconstructWeights(reflectanceDrawable, framebuffer, viewCount);

                // Calculate the error in preparation for normal estimation.
                double previousErrorLocal = error;
                updateError(errorCalcDrawable, imageReconstructionFramebuffer);

                if (DEBUG)
                {
                    System.out.println("--------------------------------------------------");
                    System.out.println("Error: " + error);
                    System.out.println("(Previous error: " + previousErrorLocal + ')');
                    System.out.println("--------------------------------------------------");
                    System.out.println();
                }

                // Prepare for normal estimation on the GPU.
                updateGraphicsResources(weightMaps, weightMask, basisMaps, diffuseUniformBuffer);

                if (NORMAL_REFINEMENT)
                {
                    // Set damping factor to 1.0 initially at each position.
                    frontNormalFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

                    // Keep track of whether each normal update is actually an improvement.
                    previousErrorLocal = error;

                    int unsuccessfulIterations = 0;

                    do
                    {
                        if (error > previousErrorLocal)
                        {
                            // Revert error calculations to what they were before attempting to optimize normals.
                            error = previousErrorLocal;
                        }

                        // Update normal estimation program to use the new front buffer.
                        normalEstimationProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                        normalEstimationProgram.setTexture("dampingTex", frontNormalFramebuffer.getColorAttachmentTexture(1));

                        // Estimate new normals.
                        reconstructNormals(normalEstimationDrawable, backNormalFramebuffer);

                        // Swap framebuffers for normal map.
                        FramebufferObject<ContextType> tmp = frontNormalFramebuffer;
                        frontNormalFramebuffer = backNormalFramebuffer;
                        backNormalFramebuffer = tmp;

                        // Update program to use the new front buffer for error calculation.
                        errorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));

                        // Check if the normal update actually was an improvement.
                        previousErrorLocal = error;

                        // Calculate the error to determine if we should stop.
                        updateError(errorCalcDrawable, imageReconstructionFramebuffer);

                        System.out.println("--------------------------------------------------");
                        System.out.println("Error: " + error);

                        if (DEBUG)
                        {
                            System.out.println("(Previous error: " + previousErrorLocal + ')');
                        }
                        else
                        {
                            System.out.println("(Previous error: " + previousError + ')');
                        }

                        System.out.println("--------------------------------------------------");
                        System.out.println();

                        if (previousErrorLocal - error <= CONVERGENCE_TOLERANCE)
                        {
                            if (DEBUG)
                            {
                                saveNormalMap(frontNormalFramebuffer/*, "normal_reject.png"*/);
                            }

                            unsuccessfulIterations++;

                            if (error > previousErrorLocal)
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
                            if (DEBUG)
                            {
                                saveNormalMap(frontNormalFramebuffer);
                            }

                            unsuccessfulIterations = 0;
                        }
                    }
                    while (USE_LEVENBERG_MARQUARDT && error <= previousErrorLocal && unsuccessfulIterations < 8 /*&& dampingFactor <= 256.0f*/);

                    if (error > previousErrorLocal)
                    {
                        // Revert error calculations to the last accepted result.
                        error = previousErrorLocal;
                    }
                }

                // Fit specular so that we have a roughness estimate for masking/shadowing.
                specularTexFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f,0.0f);
                specularTexFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f,0.0f);
                specularFitDrawable.draw(PrimitiveMode.TRIANGLE_FAN, specularTexFramebuffer);

                try
                {
                    specularTexFramebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "specular.png"));
                    specularTexFramebuffer.saveColorBufferToFile(1, "PNG", new File(outputDirectory, "roughness.png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            while (previousError - error > CONVERGENCE_TOLERANCE);

            try (PrintStream rmseOut = new PrintStream(new File(outputDirectory, "rmse.txt")))
            {
                rmseOut.println("Previously calculated RMSE: " + error);

                updateGraphicsResources(weightMaps, weightMask, basisMaps, diffuseUniformBuffer);
                updateError(errorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE before hole fill: " + error);

                fillHoles();
                updateGraphicsResources(weightMaps, weightMask, basisMaps, diffuseUniformBuffer);

                updateError(errorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE after hole fill: " + error);

                errorCalcProgram.setUniform("errorGamma", 2.2f);
                updateError(errorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE after hole fill (gamma-corrected): " + error);

                saveBasisFunctions(basisImageDrawable, basisImageFramebuffer);
                saveWeightMaps();
                saveDiffuseMap();
                saveNormalMap(frontNormalFramebuffer);

                // Diffuse fit
                Drawable<ContextType> diffuseFitDrawable = createDrawable(diffuseEstimationProgram, resources);
                diffuseEstimationProgram.setTexture("basisFunctions", basisMaps);
                diffuseEstimationProgram.setTexture("weightMaps", weightMaps);
                diffuseEstimationProgram.setTexture("weightMask", weightMask);
                diffuseEstimationProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                diffuseEstimationProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

                Drawable<ContextType> diffuseHoleFillDrawable = resources.context.createDrawable(diffuseHoleFillProgram);
                diffuseHoleFillDrawable.addVertexBuffer("position", rect);
                FramebufferObject<ContextType> finalDiffuse = fillHolesShader(diffuseHoleFillDrawable, framebuffer, diffuseHoleFillFramebuffer);

                try
                {
                    finalDiffuse.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "diffuse.png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                Drawable<ContextType> finalErrorCalcDrawable = createDrawable(finalErrorCalcProgram, resources);
                finalErrorCalcProgram.setTexture("basisFunctions", basisMaps);
                finalErrorCalcProgram.setTexture("weightMaps", weightMaps);
                finalErrorCalcProgram.setTexture("weightMask", weightMask);
                finalErrorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                finalErrorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
                finalErrorCalcProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));
                finalErrorCalcProgram.setUniform("errorGamma", 1.0f);

                updateError(finalErrorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE after final diffuse estimate: " + error);

                finalErrorCalcProgram.setUniform("errorGamma", 2.2f);
                updateError(finalErrorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE after final diffuse estimate (gamma-corrected): " + error);

                Drawable<ContextType> ggxErrorCalcDrawable = createDrawable(ggxErrorCalcProgram, resources);
                ggxErrorCalcProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                ggxErrorCalcProgram.setTexture("specularEstimate", specularTexFramebuffer.getColorAttachmentTexture(0));
                ggxErrorCalcProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
                ggxErrorCalcProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));
                ggxErrorCalcProgram.setUniform("errorGamma", 1.0f);
                updateError(ggxErrorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE for GGX fit: " + error);

                ggxErrorCalcProgram.setUniform("errorGamma", 2.2f);
                updateError(ggxErrorCalcDrawable, imageReconstructionFramebuffer);
                rmseOut.println("RMSE for GGX fit (gamma-corrected): " + error);

                // Render and save images using more accurate basis function reconstruction.
                Drawable<ContextType> imageReconstructionDrawable = createDrawable(imageReconstructionProgram, resources);
                imageReconstructionProgram.setTexture("basisFunctions", basisMaps);
                imageReconstructionProgram.setTexture("weightMaps", weightMaps);
                imageReconstructionProgram.setTexture("weightMask", weightMask);
                imageReconstructionProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                imageReconstructionProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
                imageReconstructionProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));

                reconstructImages(imageReconstructionDrawable, imageReconstructionFramebuffer, resources.viewSet, "reconstructions");

                // Fit specular textures after filling holes
                specularTexFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f,0.0f);
                specularTexFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f,0.0f);
                specularFitDrawable.draw(PrimitiveMode.TRIANGLE_FAN, specularTexFramebuffer);

                try
                {
                    specularTexFramebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "specular.png"));
                    specularTexFramebuffer.saveColorBufferToFile(1, "PNG", new File(outputDirectory, "roughness.png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                // Render and save images using parameterized fit.
                Drawable<ContextType> fittedImageReconstructionDrawable = createDrawable(fittedImageReconstructionProgram, resources);
                fittedImageReconstructionProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
                fittedImageReconstructionProgram.setTexture("specularEstimate", specularTexFramebuffer.getColorAttachmentTexture(0));
                fittedImageReconstructionProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));
                fittedImageReconstructionProgram.setTexture("diffuseEstimate", finalDiffuse.getColorAttachmentTexture(0));

                reconstructImages(fittedImageReconstructionDrawable, imageReconstructionFramebuffer, resources.viewSet, "fitted");
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        catch(FileNotFoundException e) // thrown by createReflectanceProgram
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
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && this.settingsModel.getBoolean("shadowsEnabled"))
            .createProgram();

        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>>
        Program<ContextType> createReflectanceProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/extractReflectance.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && this.settingsModel.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .createProgram();

        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>>
        Program<ContextType> createNormalEstimationProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateNormals.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && this.settingsModel.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("USE_LEVENBERG_MARQUARDT", USE_LEVENBERG_MARQUARDT ? 1 : 0)
            .define("BASIS_COUNT", BASIS_COUNT)
            .createProgram();

        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/colorappearance/imgspace_multi_as_single.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/errorCalc.frag"))
            .define("VISIBILITY_TEST_ENABLED", 0)
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", BASIS_COUNT)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createFinalErrorCalcProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/colorappearance/imgspace_multi_as_single.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/finalErrorCalc.frag"))
            .define("VISIBILITY_TEST_ENABLED", 0)
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", BASIS_COUNT)
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
            .define("BASIS_COUNT", BASIS_COUNT)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>>
    Program<ContextType> createDiffuseEstimationProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateDiffuse.frag"))
            .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && this.settingsModel.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", BASIS_COUNT)
            .createProgram();

        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>>
        Program<ContextType> createImageReconstructionProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/reconstructImage.frag"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", ORIGINAL_NAM_METHOD ? 0 : 1)
            .define("BASIS_COUNT", BASIS_COUNT)
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
            .define("BASIS_COUNT", BASIS_COUNT)
            .createProgram();

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType>
        createDrawable(Program<ContextType> program, IBRResources<ContextType> resources)
    {
        Drawable<ContextType> drawable = program.getContext().createDrawable(program);
        drawable.addVertexBuffer("position", resources.positionBuffer);
        drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        drawable.addVertexBuffer("normal", resources.normalBuffer);
        drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        return drawable;
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
                framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "average.png"));
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
            firstCenterIndex = random.nextInt(width * height);
        }
        while(averages[4 * firstCenterIndex + 3] < 1.0); // Make sure the center chosen is valid.

        DoubleVector3[] centers = new DoubleVector3[BASIS_COUNT];
        centers[0] = new DoubleVector3(averages[4 * firstCenterIndex], averages[4 * firstCenterIndex + 1], averages[4 * firstCenterIndex + 2]);

        // Populate a CDF for the purpose of randomly selecting from a weighted probability distribution.
        double[] cdf = new double[width * height + 1];

        for (int b = 1; b < BASIS_COUNT; b++)
        {
            cdf[0] = 0.0;

            for (int p = 0; p < width * height; p++)
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

            double x = random.nextDouble() * cdf[width * height - 1];

            //noinspection FloatingPointEquality
            if (x >= cdf[width * height - 1]) // It's possible but extremely unlikely that floating-point rounding would cause this to happen.
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
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            System.out.println(centers[b]);
        }

        // Initialization is done; now it's time to iterate.
        boolean changed;
        do
        {
            // Initialize sums to zero.
            DoubleVector4[] sums = IntStream.range(0, BASIS_COUNT).mapToObj(i -> DoubleVector4.ZERO_DIRECTION).toArray(DoubleVector4[]::new);

            for (int p = 0; p < width * height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < BASIS_COUNT; b++)
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
            for (int b = 0; b < BASIS_COUNT; b++)
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

        for (int p = 0; p < width * height; p++)
        {
            // Initialize weights to zero.
            weightSolutions[p].zero();

            if (averages[4 * p + 3] > 0.0)
            {
                int bMin = -1;

                double minDistance = Double.MAX_VALUE;

                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        bMin = b;
                    }
                }

                // Set weight to one for the cluster that each pixel belongs to.
                weightSolutions[p].set(bMin, 1.0);
            }
        }

        System.out.println("Refined centers:");
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            System.out.println(centers[b]);
        }

        if (DEBUG)
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];
            for (int p = 0; p < width * height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < BASIS_COUNT; b++)
                    {
                        double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            bMin = b;
                        }
                    }

                    // Flip vertically
                    int weightDataIndex = p % width + width * (height - p / width - 1);

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
                ImageIO.write(weightImg, "PNG", new File(outputDirectory, "k-means.png"));
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

        for (int b = 0; b < BASIS_COUNT; b++)
        {
            int bCopy = b;

            // Only update if the BRDF has non-zero elements.
            if (IntStream.range(0, MICROFACET_DISTRIBUTION_RESOLUTION + 1).anyMatch(
                i -> brdfSolutionRed.get(bCopy + BASIS_COUNT * i) > 0
                    || brdfSolutionGreen.get(bCopy + BASIS_COUNT * i) > 0
                    || brdfSolutionBlue.get(bCopy + BASIS_COUNT * i) > 0))
            {
                DoubleVector3 baseColor = new DoubleVector3(brdfSolutionRed.get(b), brdfSolutionGreen.get(b), brdfSolutionBlue.get(b));
                diffuseAlbedos[b] = baseColor.times(1.0 - METALLICITY);

                specularRed.set(MICROFACET_DISTRIBUTION_RESOLUTION, b, baseColor.x * METALLICITY);
                specularGreen.set(MICROFACET_DISTRIBUTION_RESOLUTION, b, baseColor.y * METALLICITY);
                specularBlue.set(MICROFACET_DISTRIBUTION_RESOLUTION, b, baseColor.z * METALLICITY);

                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    // f[m] = f[m+1] + estimated difference (located at index m + 1 due to diffuse component at index 0).
                    specularRed.set(m, b, specularRed.get(m + 1, b) + brdfSolutionRed.get((m + 1) * BASIS_COUNT + b));
                    specularGreen.set(m, b, specularGreen.get(m + 1, b) + brdfSolutionGreen.get((m + 1) * BASIS_COUNT + b));
                    specularBlue.set(m, b, specularBlue.get(m + 1, b) + brdfSolutionBlue.get((m + 1) * BASIS_COUNT + b));
                }
            }
        }

        if (DEBUG)
        {
            System.out.println();

            for (int b = 0; b < BASIS_COUNT; b++)
            {
                DoubleVector3 diffuseColor = new DoubleVector3(
                    brdfSolutionRed.get(b),
                    brdfSolutionGreen.get(b),
                    brdfSolutionBlue.get(b));
                System.out.println("Diffuse #" + b + ": " + diffuseColor);
            }

            System.out.println("Basis BRDFs:");

            for (int b = 0; b < BASIS_COUNT; b++)
            {
                System.out.print("Red#" + b);
                double redTotal = 0.0;
                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    redTotal += brdfSolutionRed.get((m + 1) * BASIS_COUNT + b);
                    System.out.print(redTotal);
                }

                System.out.println();

                System.out.print("Green#" + b);
                double greenTotal = 0.0;
                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    greenTotal += brdfSolutionGreen.get((m + 1) * BASIS_COUNT + b);
                    System.out.print(greenTotal);
                }
                System.out.println();

                System.out.print("Blue#" + b);
                double blueTotal = 0.0;
                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    blueTotal += brdfSolutionBlue.get((m + 1) * BASIS_COUNT + b);
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
                    SimpleMatrix contributionATA = new SimpleMatrix(BRDF_MATRIX_SIZE, BRDF_MATRIX_SIZE, DMatrixRMaj.class);
                    SimpleMatrix contributionATyRed = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
                    SimpleMatrix contributionATyGreen = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
                    SimpleMatrix contributionATyBlue = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);

                    // Get the contributions from the current view.
                    new ReflectanceMatrixBuilder(colorAndVisibility, halfwayAndGeom, weightSolutions, contributionATA,
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

            for (int b = 0; b < BASIS_COUNT; b++)
            {
                System.out.print("RHS, red for BRDF #" + b + ": ");

                System.out.print(brdfATyRed.get(b));
                for (int m = 0; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                {
                    System.out.print(", ");
                    System.out.print(brdfATyRed.get((m + 1) * BASIS_COUNT + b));
                }
                System.out.println();

                System.out.print("RHS, green for BRDF #" + b + ": ");

                System.out.print(brdfATyGreen.get(b));
                for (int m = 0; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                {
                    System.out.print(", ");
                    System.out.print(brdfATyGreen.get((m + 1) * BASIS_COUNT + b));
                }
                System.out.println();

                System.out.print("RHS, blue for BRDF #" + b + ": ");

                System.out.print(brdfATyBlue.get(b));
                for (int m = 0; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                {
                    System.out.print(", ");
                    System.out.print(brdfATyBlue.get((m + 1) * BASIS_COUNT + b));
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

        for (int p = 0; p < width * height; p++)
        {
            if (weightsValidity[p])
            {
                double median = IntStream.range(0, weightsQTrAugmented[p].getNumElements()).mapToDouble(weightsQTrAugmented[p]::get)
                    .sorted().skip(weightsQTrAugmented[p].getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
                weightSolutions[p] = NonNegativeLeastSquares.solvePremultipliedWithEqualityConstraints(
                    weightsQTQAugmented[p], weightsQTrAugmented[p], median * NNLS_TOLERANCE_SCALE, 1);
            }
        }

        System.out.println("DONE!");

        if (DEBUG)
        {
            // write out weight textures for debugging
            saveWeightMaps();

            // write out diffuse texture for debugging
            saveDiffuseMap();
        }
    }

    private <ContextType extends Context<ContextType>> void buildWeightMatrices(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        // Initially assume that all texels are invalid.
        Arrays.fill(weightsValidity, false);

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
            IntStream.range(0, width * height).parallel().forEach(p ->
            {
                // Skip samples that aren't visible or are otherwise invalid.
                if (colorAndVisibility[4 * p + 3] > 0)
                {
                    // Any time we have a visible, valid sample, mark that the corresponding texel is valid.
                    weightsValidity[p] = true;

                    // Precalculate frequently used values.

                    // For original Nam 2018 version, weights were optimized against reflectance, not reflected radiance,
                    // so we don't want to multiply by n dot l when attempting to reproduce that version.
                    double nDotLSquared = ORIGINAL_NAM_METHOD ? 1.0 : halfwayAndGeomAndWeights[4 * p + 3] * halfwayAndGeomAndWeights[4 * p + 3];

                    double weightSquared = halfwayAndGeomAndWeights[4 * p + 2] * halfwayAndGeomAndWeights[4 * p + 2];
                    double geomFactor = halfwayAndGeomAndWeights[4 * p + 1];
                    double mExact = halfwayAndGeomAndWeights[4 * p] * MICROFACET_DISTRIBUTION_RESOLUTION;
                    int m1 = (int)Math.floor(mExact);
                    int m2 = m1 + 1;
                    double t = mExact - m1;
                    DoubleVector3 fActual = new DoubleVector3(colorAndVisibility[4 * p], colorAndVisibility[4 * p + 1], colorAndVisibility[4 * p + 2]);

                    for (int b1 = 0; b1 < BASIS_COUNT; b1++)
                    {
                        // Evaluate the first basis BRDF.
                        DoubleVector3 f1 = diffuseAlbedos[b1].dividedBy(PI);

                        if (m1 < MICROFACET_DISTRIBUTION_RESOLUTION)
                        {
                            f1 = f1.plus(new DoubleVector3(specularRed.get(m1, b1), specularGreen.get(m1, b1), specularBlue.get(m1, b1))
                                .times(1.0 - t)
                                .plus(new DoubleVector3(specularRed.get(m2, b1), specularGreen.get(m2, b1), specularBlue.get(m2, b1))
                                    .times(t))
                                .times(geomFactor));
                        }
                        else if (METALLICITY > 0.0f)
                        {
                            f1 = f1.plus(new DoubleVector3(
                                    specularRed.get(MICROFACET_DISTRIBUTION_RESOLUTION, b1),
                                    specularGreen.get(MICROFACET_DISTRIBUTION_RESOLUTION, b1),
                                    specularBlue.get(MICROFACET_DISTRIBUTION_RESOLUTION, b1))
                                .times(geomFactor));
                        }

                        // Store the weighted product of the basis BRDF and the actual BRDF in the vector.
                        weightsQTrAugmented[p].set(b1, weightsQTrAugmented[p].get(b1) + weightSquared * nDotLSquared * f1.dot(fActual));

                        for (int b2 = 0; b2 < BASIS_COUNT; b2++)
                        {
                            // Evaluate the second basis BRDF.
                            DoubleVector3 f2 = diffuseAlbedos[b2].dividedBy(PI);

                            if (m1 < MICROFACET_DISTRIBUTION_RESOLUTION)
                            {
                                f2 = f2.plus(new DoubleVector3(specularRed.get(m1, b2), specularGreen.get(m1, b2), specularBlue.get(m1, b2))
                                    .times(1.0 - t)
                                    .plus(new DoubleVector3(specularRed.get(m2, b2), specularGreen.get(m2, b2), specularBlue.get(m2, b2))
                                        .times(t))
                                    .times(geomFactor));
                            }
                            else if (METALLICITY > 0.0f)
                            {
                                f2 = f2.plus(new DoubleVector3(
                                        specularRed.get(MICROFACET_DISTRIBUTION_RESOLUTION, b2),
                                        specularGreen.get(MICROFACET_DISTRIBUTION_RESOLUTION, b2),
                                        specularBlue.get(MICROFACET_DISTRIBUTION_RESOLUTION, b2))
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

    private <ContextType extends Context<ContextType>> void updateGraphicsResources(
        Texture3D<ContextType> weightMaps, Texture2D<ContextType> weightMask, Texture2D<ContextType> basisMaps, UniformBuffer<ContextType> diffuseUniformBuffer)
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer weightMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 1, width * height);
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3, BASIS_COUNT * (MICROFACET_DISTRIBUTION_RESOLUTION + 1));
        NativeVectorBuffer diffuseNativeBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, BASIS_COUNT);

        // Load weight mask first.
        for (int p = 0; p < width * height; p++)
        {
            weightMapBuffer.set(p, 0, weightsValidity[p] ? 1.0 : 0.0);
        }

        weightMask.load(weightMapBuffer);

        for (int b = 0; b < BASIS_COUNT; b++)
        {
            // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
            for (int p = 0; p < width * height; p++)
            {
                weightMapBuffer.set(p, 0, weightSolutions[p].get(b));
            }

            // Immediately load the weight map so that we can reuse the local memory buffer.
            weightMaps.loadLayer(b, weightMapBuffer);

            // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
            for (int m = 0; m <= MICROFACET_DISTRIBUTION_RESOLUTION; m++)
            {
                // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                basisMapBuffer.set(m + (MICROFACET_DISTRIBUTION_RESOLUTION + 1) * b, 0, specularRed.get(m, b));
                basisMapBuffer.set(m + (MICROFACET_DISTRIBUTION_RESOLUTION + 1) * b, 1, specularGreen.get(m, b));
                basisMapBuffer.set(m + (MICROFACET_DISTRIBUTION_RESOLUTION + 1) * b, 2, specularBlue.get(m, b));
            }

            // Store each channel of the diffuse albedo in the local buffer.
            diffuseNativeBuffer.set(b, 0, diffuseAlbedos[b].x);
            diffuseNativeBuffer.set(b, 1, diffuseAlbedos[b].y);
            diffuseNativeBuffer.set(b, 2, diffuseAlbedos[b].z);
            diffuseNativeBuffer.set(b, 3, 1.0f);
        }

        // Send the basis functions to the GPU.
        basisMaps.load(basisMapBuffer);

        // Send the diffuse albedos to the GPU.
        diffuseUniformBuffer.setData(diffuseNativeBuffer);
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

    private <ContextType extends Context<ContextType>> void updateError(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        if (DEBUG)
        {
            System.out.println("Calculating error...");
        }

        // Clear framebuffer
        framebuffer.clearDepthBuffer();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        // Run shader program to fill framebuffer with per-pixel error.
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        // Copy framebuffer from GPU to main memory.
        float[] pixelErrors = framebuffer.readFloatingPointColorBufferRGBA(0);

        double errorTotal = 0.0;
        int validCount = 0;

        // Add up per-pixel error.
        for (int p = 0; p < width * height; p++)
        {
            if (pixelErrors[4 * p + 3] > 0)
            {
                errorTotal += pixelErrors[4 * p];
                validCount += pixelErrors[4 * p + 3];
            }
        }

        error = Math.sqrt(errorTotal / validCount);
    }

    private <ContextType extends Context<ContextType>>
    FramebufferObject<ContextType> fillHolesShader(Drawable<ContextType> holeFillDrawable, FramebufferObject<ContextType> initFrontFramebuffer,
        FramebufferObject<ContextType> initBackFramebuffer)
    {
        FramebufferObject<ContextType> frontFramebuffer = initFrontFramebuffer;
        FramebufferObject<ContextType> backFramebuffer = initBackFramebuffer;

        for (int i = 0; i < Math.max(width, height); i++)
        {
            holeFillDrawable.program().setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
            holeFillDrawable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);

            FramebufferObject<ContextType> tmp = frontFramebuffer;
            frontFramebuffer = backFramebuffer;
            backFramebuffer = tmp;
        }

        return frontFramebuffer;
    }

    private void fillHoles()
    {
        // Fill holes
        // TODO Quick hack; should be replaced with something more robust.
        System.out.println("Filling holes...");

        for (int i = 0; i < Math.max(width, height); i++)
        {
            Collection<Integer> filledPositions = new HashSet<>(256);
            for (int p = 0; p < weightsValidity.length; p++)
            {
                if (!weightsValidity[p])
                {
                    int left = (weightsValidity.length + p - 1) % weightsValidity.length;
                    int right = (p + 1) % weightsValidity.length;
                    int up = (weightsValidity.length + p - width) % weightsValidity.length;
                    int down = (p + width) % weightsValidity.length;

                    int count = 0;

                    for (int b = 0; b < BASIS_COUNT; b++)
                    {
                        count = 0;
                        double sum = 0.0;

                        if (weightsValidity[left])
                        {
                            sum += weightSolutions[left].get(b);
                            count++;
                        }

                        if (weightsValidity[right])
                        {
                            sum += weightSolutions[right].get(b);
                            count++;
                        }

                        if (weightsValidity[up])
                        {
                            sum += weightSolutions[up].get(b);
                            count++;
                        }

                        if (weightsValidity[down])
                        {
                            sum += weightSolutions[down].get(b);
                            count++;
                        }

                        if (sum > 0.0)
                        {
                            weightSolutions[p].set(b, sum / count);
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
                weightsValidity[p] = true;
            }
        }

        System.out.println("DONE!");
    }

    private <ContextType extends Context<ContextType>> void saveBasisFunctions(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        // Text file format
        try (PrintStream out = new PrintStream(new File(outputDirectory, "basisFunctions.csv")))
        {
            for (int b = 0; b < BASIS_COUNT; b++)
            {
                out.print("Red#" + b);
                for (int m = 0; m <= MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                {
                    out.print(", ");
                    out.print(specularRed.get(m, b));
                }
                out.println();

                out.print("Green#" + b);
                for (int m = 0; m <= MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                {
                    out.print(", ");
                    out.print(specularGreen.get(m, b));
                }
                out.println();

                out.print("Blue#" + b);
                for (int m = 0; m <= MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                {
                    out.print(", ");
                    out.print(specularBlue.get(m, b));
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
            for (int i = 0; i < BASIS_COUNT; i++)
            {
                drawable.program().setUniform("basisIndex", i);
                drawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, String.format("basis_%02d.png", i)));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void saveWeightMaps()
    {
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];

            for (int p = 0; p < width * height; p++)
            {
                float weight = (float)weightSolutions[p].get(b);

                // Flip vertically
                int dataBufferIndex = p % width + width * (height - p / width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(outputDirectory, String.format("weights%02d.png", b)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void saveDiffuseMap()
    {
        BufferedImage diffuseImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] diffuseDataPacked = new int[width * height];
        for (int p = 0; p < width * height; p++)
        {
            DoubleVector4 diffuseSum = DoubleVector4.ZERO_DIRECTION;

            for (int b = 0; b < BASIS_COUNT; b++)
            {
                diffuseSum = diffuseSum.plus(diffuseAlbedos[b].asVector4(1.0)
                    .times(weightSolutions[p].get(b)));
            }

            if (diffuseSum.w > 0)
            {
                DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / GAMMA)));

                // Flip vertically
                int dataBufferIndex = p % width + width * (height - p / width - 1);
                diffuseDataPacked[dataBufferIndex] = new Color((float) diffuseAvgGamma.x, (float) diffuseAvgGamma.y, (float) diffuseAvgGamma.z).getRGB();
            }
        }

        diffuseImg.setRGB(0, 0, diffuseImg.getWidth(), diffuseImg.getHeight(), diffuseDataPacked, 0, diffuseImg.getWidth());

        try
        {
            ImageIO.write(diffuseImg, "PNG", new File(outputDirectory, "diffuse_frombasis.png"));
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
            framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private <ContextType extends Context<ContextType>> void reconstructImages(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, ViewSet viewSet, String folderName)
    {
        new File(outputDirectory, folderName).mkdir();

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
                    new File(new File(outputDirectory, folderName), filename));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
