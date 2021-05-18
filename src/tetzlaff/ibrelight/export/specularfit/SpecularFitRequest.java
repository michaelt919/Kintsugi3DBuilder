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
import java.util.stream.IntStream;


import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.util.NonNegativeLeastSquares;

import static java.lang.Math.PI;

/**
 * Implement specular fit using algorithm described by Nam et al., 2018
 */
public class SpecularFitRequest extends TextureFitRequest
{
    static final boolean DEBUG = true;
    static final boolean ORIGINAL_NAM_METHOD = false;
    static final boolean USE_LEVENBERG_MARQUARDT = !ORIGINAL_NAM_METHOD;

    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;
    private static final double CONVERGENCE_TOLERANCE = 0.0001;

    private static final boolean NORMAL_REFINEMENT = true;

    private static final double METALLICITY = 0.0f; // Implemented and minimally tested but doesn't seem to make much difference.

    private final SpecularFitSettings settings;

    private final ShaderBasedErrorCalculator errorCalculator;

    private final SimpleMatrix[] weightsQTQAugmented;
    private final SimpleMatrix[] weightsQTrAugmented;

    private final SpecularFitSolution solution;
    public SpecularFitRequest(SpecularFitSettings settings)
    {
        super(settings);

        this.settings = settings;
        solution = new SpecularFitSolution(settings);

        errorCalculator = new ShaderBasedErrorCalculator(settings.width * settings.height);

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

        try
        (
            // Shader program to extract reflectance and other geometric info
            Program<ContextType> reflectanceProgram = createReflectanceProgram(programFactory);

            // Compare fitted models against actual photographs
            Program<ContextType> errorCalcProgram = createErrorCalcProgram(programFactory);

            // Rectangle vertex buffer
            VertexBuffer<ContextType> rect = context.createRectangle();

            // Framebuffer used for extracting reflectance and related geometric data
            FramebufferObject<ContextType> framebuffer = context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject();

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
            SpecularFitResources<ContextType> specularFitResources = new SpecularFitResources<ContextType>(context, settings)
        )
        {
            // Initialize weights using K-means.
            new SpecularFitInitializer<>(resources, programFactory, framebuffer, settings).initialize(solution);

            normalFramebuffer1.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
            normalFramebuffer2.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            // Double buffering since we need the previous normal estimate to generate the next normal estimate.
            FramebufferObject<ContextType> frontNormalFramebuffer = normalFramebuffer1;
            FramebufferObject<ContextType> backNormalFramebuffer = normalFramebuffer2;

            Drawable<ContextType> reflectanceDrawable = resources.createDrawable(reflectanceProgram);
            reflectanceProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));
            reflectanceProgram.setTexture("roughnessEstimate", specularTexFramebuffer.getColorAttachmentTexture(1));

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

            int viewCount = resources.viewSet.getCameraPoseCount();

            double previousError;

            BRDFReconstruction brdfReconstruction = new BRDFReconstruction(settings, METALLICITY);

            do
            {
                previousError = errorCalculator.getError();

                // Use the current front normal buffer for extracting reflectance information.
                reflectanceProgram.setTexture("normalEstimate", frontNormalFramebuffer.getColorAttachmentTexture(0));

                brdfReconstruction.execute(resources, reflectanceDrawable, framebuffer, solution);

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

            new SpecularFitFinalizer(settings).finalize(programFactory, resources, rect, framebuffer, tempFramebuffer, specularRoughnessFitDrawable,
                specularTexFramebuffer, errorCalcDrawable, errorCalculator, solution, specularFitResources, frontNormalFramebuffer.getColorAttachmentTexture(0));
        }
        catch(FileNotFoundException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>>
        Program<ContextType> createReflectanceProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
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
            solution.saveWeightMaps();

            // write out diffuse texture for debugging
            solution.saveDiffuseMap(settings.additional.getFloat("gamma"));
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
}
