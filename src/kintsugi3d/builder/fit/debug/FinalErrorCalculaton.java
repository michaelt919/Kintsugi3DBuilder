/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.util.stream.IntStream;

import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.resources.ibr.IBRResources;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.DoubleVector2;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.optimization.ShaderBasedErrorCalculator;
import org.lwjgl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module that performs some final steps to finish a specular fit: filling holes in the weight maps, and calculating some final error statistics.
 * TODO this class doesn't have a well defined identity; should probably be refactored.
 */
public final class FinalErrorCalculaton
{
    private static final Logger log = LoggerFactory.getLogger(FinalErrorCalculaton.class);
    private static final FinalErrorCalculaton INSTANCE = new FinalErrorCalculaton();

    public static FinalErrorCalculaton getInstance()
    {
        return INSTANCE;
    }

    private static final boolean CALCULATE_NORMAL_RMSE = true;

    private FinalErrorCalculaton()
    {
    }

    /**
     *
     * @param resources
     * @param specularFit
     * @param rmseOut Text file containing error information
     * @param <ContextType>
     */
    public <ContextType extends Context<ContextType>> void validateNormalMap(
        IBRResources<ContextType> resources, SpecularMaterialResources<ContextType> specularFit, PrintStream rmseOut)
    {
        if (CALCULATE_NORMAL_RMSE && resources.getSpecularMaterialResources().getNormalMap() != null)
        {
            try (ProgramObject<ContextType> textureRectProgram = resources.getContext().getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_dynamic.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/common/texture.frag"))
                .createProgram();
                 FramebufferObject<ContextType> textureRectFBO =
                    resources.getContext().buildFramebufferObject(specularFit.getWidth(), specularFit.getHeight()).addColorAttachment().createFramebufferObject();
                Drawable<ContextType> textureRect = resources.createDrawable(textureRectProgram))
            {
                textureRectProgram.setTexture("tex", resources.getSpecularMaterialResources().getNormalMap());
                textureRectFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                textureRect.draw(PrimitiveMode.TRIANGLE_FAN, textureRectFBO);
//                    textureRectFBO.saveColorBufferToFile(0, "PNG", new File(textureFitSettings.outputDirectory, "test_normalGT.png"));

                float[] groundTruth = textureRectFBO.getTextureReaderForColorAttachment(0).readFloatingPointRGBA();
                float[] estimate = specularFit.getNormalMap().getColorTextureReader().readFloatingPointRGBA();

                double rmse = Math.sqrt( // root
                    IntStream.range(0, groundTruth.length / 4)
                        // only count texels that correspond to actual texture coordinates on the model (others will be transparent after rasterizing the ground truth)
                        .filter(p -> groundTruth[4 * p + 3] > 0.0)
                        .mapToDouble(p ->
                        {
                            DoubleVector2 groundTruthXY = new DoubleVector2(groundTruth[4 * p] * 2 - 1, groundTruth[4 * p + 1] * 2 - 1);
                            // Unpack normal vectors
                            DoubleVector3 groundTruthDir = groundTruthXY.asVector3(1 - groundTruthXY.dot(groundTruthXY));
                            DoubleVector3 estimateDir = new DoubleVector3(
                                estimate[4 * p] * 2 - 1, estimate[4 * p + 1] * 2 - 1, estimate[4 * p + 2] * 2 - 1).normalized();

                            DoubleVector3 error = groundTruthDir.minus(estimateDir);
                            return error.dot(error); // sum squared error
                        })
                        .average().orElse(0.0)); // mean

                log.info("Normal map ground truth RMSE: " + rmse);

                // Print out RMSE for normal map ground truth
                rmseOut.println("Normal map ground truth RMSE: " + rmse);
            }
            catch (IOException e)
            {
                log.error("An error occurred while validating normal map:", e);
            }
        }
    }

    public <ContextType extends Context<ContextType>> void calculateFinalErrorMetrics(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory,
        SpecularMaterialResources<ContextType> specularFit, ShaderBasedErrorCalculator<ContextType> basisErrorCalculator,
        PrintStream rmseOut)
    {
        try (ProgramObject<ContextType> finalErrorCalcProgram = createFinalErrorCalcProgram(resources, programFactory);
            Drawable<ContextType> finalErrorCalcDrawable = resources.createDrawable(finalErrorCalcProgram))
        {
            // Calculate linear RMSE using basis diffuse
            basisErrorCalculator.update();
            rmseOut.println("RMSE using basis diffuse (linear): " + basisErrorCalculator.getReport().getError());

            // Calculate sRGB RMSE using basis diffuse
            basisErrorCalculator.getProgram().setUniform("sRGB", true);
            basisErrorCalculator.update();
            rmseOut.println("RMSE using basis diffuse (sRGB): " + basisErrorCalculator.getReport().getError());

            // Setup drawable that uses the final diffuse texture (not limited to basis colors)
            specularFit.getBasisResources().useWithShaderProgram(finalErrorCalcProgram);
            specularFit.getBasisWeightResources().useWithShaderProgram(finalErrorCalcProgram);
            finalErrorCalcProgram.setTexture("normalMap", specularFit.getNormalMap());
            finalErrorCalcProgram.setTexture("roughnessMap", specularFit.getSpecularRoughnessMap());
            finalErrorCalcProgram.setTexture("diffuseMap", specularFit.getDiffuseMap());

            if (specularFit.getConstantMap() != null)
            {
                // TODO add support for constant maps in error estimation
                finalErrorCalcProgram.setTexture("constantEstimate", specularFit.getConstantMap());
            }

            finalErrorCalcProgram.setUniform("sRGB", false);

            // Reuse errorCalculator's framebuffer as a scratch framebuffer (for efficiency)
            Framebuffer<ContextType> scratchFramebuffer = basisErrorCalculator.getFramebuffer();

            rmseOut.println("Final RMSE with final diffuse estimate (linear): " +
                runFinalErrorCalculation(finalErrorCalcDrawable, scratchFramebuffer, resources.getViewSet().getCameraPoseCount()));

            finalErrorCalcProgram.setUniform("sRGB", true);
            rmseOut.println("Final RMSE with final diffuse estimate (sRGB): " +
                runFinalErrorCalculation(finalErrorCalcDrawable, scratchFramebuffer, resources.getViewSet().getCameraPoseCount()));

            // Calculate error using the GGX fit rather than the basis functions.
            calculateGGXRMSE(resources, programFactory, specularFit, scratchFramebuffer, rmseOut);
        }
        catch (IOException e)
        {
            log.error("An error occurred while calculating error metrics:", e);
        }
    }

    /**
     * Calculates RMSE for GGX.
     * Can be used standalone (i.e. when loading the optimized specular basis from a file)
     * @param resources
     * @param specularFit
     * @param scratchFramebuffer
     * @param rmseOut
     * @param <ContextType>
     * @throws FileNotFoundException
     */
    private <ContextType extends Context<ContextType>> void calculateGGXRMSE(
            ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory,
            SpecularMaterialResources<ContextType> specularFit, Framebuffer<ContextType> scratchFramebuffer, PrintStream rmseOut)
        throws IOException
    {
        try (ProgramObject<ContextType> ggxErrorCalcProgram = createGGXErrorCalcProgram(resources, programFactory);
            Drawable<ContextType> ggxErrorCalcDrawable = resources.createDrawable(ggxErrorCalcProgram))
        {
            ggxErrorCalcProgram.setTexture("normalMap", specularFit.getNormalMap());
            ggxErrorCalcProgram.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
            ggxErrorCalcProgram.setTexture("roughnessMap", specularFit.getSpecularRoughnessMap());
            ggxErrorCalcProgram.setTexture("diffuseMap", specularFit.getDiffuseMap());

            if (specularFit.getConstantMap() != null)
            {
                // TODO add support for constant maps in error estimation
                ggxErrorCalcProgram.setTexture("constantEstimate", specularFit.getConstantMap());
            }

            ggxErrorCalcProgram.setUniform("sRGB", false);

            rmseOut.println("RMSE for GGX fit (linear): " +
                runFinalErrorCalculation(ggxErrorCalcDrawable, scratchFramebuffer, resources.getViewSet().getCameraPoseCount()));

            ggxErrorCalcProgram.setUniform("sRGB", true);
            rmseOut.println("RMSE for GGX fit (sRGB): " +
                runFinalErrorCalculation(ggxErrorCalcDrawable, scratchFramebuffer, resources.getViewSet().getCameraPoseCount()));
        }
    }

    private static class WeightedError
    {
        double error;
        double weight;

        WeightedError(double error, double weight)
        {
            this.error = error;
            this.weight = weight;
        }
    }

    private static <ContextType extends Context<ContextType>>
    double runFinalErrorCalculation(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        WeightedError errorTotal = new WeightedError(0, 0);

        FramebufferSize size = framebuffer.getSize();
        FloatBuffer pixelErrors = BufferUtils.createFloatBuffer(size.width * size.height * 4);

        for (int k = 0; k < viewCount; k++)
        {
            drawable.program().setUniform("viewIndex", k);

            // Clear framebuffer
            framebuffer.clearDepthBuffer();
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel error.
            drawable.draw(framebuffer);

            // Copy framebuffer from GPU to main memory.
            framebuffer.getTextureReaderForColorAttachment(0).readFloatingPointRGBA(pixelErrors);
            //            float[] pixelErrors = framebuffer.readFloatingPointColorBufferRGBA(0);

            // Add up per-pixel error.
            WeightedError errorViewTotal = IntStream.range(0, pixelErrors.limit() / 4)
                .parallel()
                .filter(p -> pixelErrors.get(4 * p + 3) > 0)
                .collect(() -> new WeightedError(0, 0),
                    (total, p) ->
                    {
                        total.error += pixelErrors.get(4 * p);
                        total.weight += pixelErrors.get(4 * p + 3);
                    },
                    (total1, total2) ->
                    {
                        total1.error += total2.error;
                        total1.weight += total2.weight;
                    });

            // Add results from view to final total
            errorTotal.error += errorViewTotal.error;
            errorTotal.weight += errorViewTotal.weight;
        }

        return Math.sqrt(errorTotal.error / errorTotal.weight);
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createFinalErrorCalcProgram(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory) throws IOException
    {
        return programFactory.createProgram(resources,
            new File("shaders/colorappearance/imgspace_multi_as_single.vert"),
            new File("shaders/specularfit/finalErrorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createGGXErrorCalcProgram(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory) throws IOException
    {
        return programFactory.createProgram(resources,
            new File("shaders/colorappearance/imgspace_multi_as_single.vert"),
            new File("shaders/specularfit/ggxErrorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }

}
