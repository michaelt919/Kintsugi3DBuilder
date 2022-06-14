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
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.IntStream;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedErrorCalculator;

/**
 * A module that performs some final steps to finish a specular fit: filling holes in the weight maps, and calculating some final error statistics.
 */
public class SpecularFitFinalizer
{
    private final SpecularFitSettings settings;

    private static final boolean CALCULATE_NORMAL_RMSE = true;

    public SpecularFitFinalizer(SpecularFitSettings settings)
    {
        this.settings = settings;
    }

    public <ContextType extends Context<ContextType>> void execute(
        SpecularFitSolution solution, IBRResources<ContextType> resources, SpecularFit<ContextType> specularFit,
        Framebuffer<ContextType> scratchFramebuffer, ReadonlyErrorReport lastErrorReport, Drawable<ContextType> errorCalcDrawable)
    {
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(resources, settings);

        try (
            // Text file containing error information
            PrintStream rmseOut = new PrintStream(new File(settings.outputDirectory, "rmse.txt"));

            // Error calculation shader programs
            Program<ContextType> finalErrorCalcProgram = createFinalErrorCalcProgram(programFactory);
            Program<ContextType> ggxErrorCalcProgram = createGGXErrorCalcProgram(programFactory);
        )
        {
            if (CALCULATE_NORMAL_RMSE && resources.normalTexture != null)
            {
                try (Program<ContextType> textureRectProgram = resources.context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                    .addShader(ShaderType.FRAGMENT, new File("shaders/common/texture.frag"))
                    .createProgram();
                    FramebufferObject<ContextType> textureRectFBO =
                        resources.context.buildFramebufferObject(settings.width, settings.height).addColorAttachment().createFramebufferObject())
                {
                    // Use the real geometry rather than a rectangle so that the normal map is masked properly for the part of the normal map used.
                    Drawable<ContextType> textureRect = resources.createDrawable(textureRectProgram);
                    textureRectProgram.setTexture("tex", resources.normalTexture);
                    textureRectFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                    textureRect.draw(PrimitiveMode.TRIANGLE_FAN, textureRectFBO);
//                    textureRectFBO.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "test_normalGT.png"));

                    float[] groundTruth = textureRectFBO.readFloatingPointColorBufferRGBA(0);
                    float[] estimate = specularFit.normalOptimization.readNormalMap();

                    double rmse = Math.sqrt( // root
                        IntStream.range(0, groundTruth.length / 4)
                            .filter(p -> groundTruth[4 * p + 3] > 0.0) // only count texels that had enough samples to generate a solution
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

                    System.out.println("Normal map ground truth RMSE: " + rmse);

                    // Print out RMSE for normal map ground truth
                    rmseOut.println("Normal map ground truth RMSE: " + rmse);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            // Print out RMSE from the penultimate iteration (to verify convergence)
            rmseOut.println("Previously calculated RMSE: " + lastErrorReport.getError());

            // Create an error calculator that will be reused for different computations.
            ShaderBasedErrorCalculator errorCalculator = new ShaderBasedErrorCalculator(settings.width * settings.height);

            // Calculate the final RMSE from the raw result
            specularFit.basisResources.updateFromSolution(solution);
            errorCalcDrawable.program().setTexture("normalEstimate", specularFit.getNormalMap());

            errorCalculator.update(errorCalcDrawable, scratchFramebuffer);
            rmseOut.println("RMSE before hole fill: " + errorCalculator.getReport().getError());

            // Fill holes in the weight map
            fillHoles(solution);

            // Save the weight map and preliminary diffuse result after filling holes
            solution.saveWeightMaps();
            solution.saveDiffuseMap(settings.additional.getFloat("gamma"));

            // Update the GPU resources with the hole-filled weight maps.
            specularFit.basisResources.updateFromSolution(solution);

            // Fit specular textures after filling holes
            specularFit.roughnessOptimization.execute();
            specularFit.roughnessOptimization.saveTextures();

            // Calculate RMSE after filling holes
            errorCalculator.update(errorCalcDrawable, scratchFramebuffer);
            rmseOut.println("RMSE after hole fill: " + errorCalculator.getReport().getError());

            // Calculate gamma-corrected RMSE
            errorCalcDrawable.program().setUniform("errorGamma", 2.2f);
            errorCalculator.update(errorCalcDrawable, scratchFramebuffer);
            rmseOut.println("RMSE after hole fill (gamma-corrected): " + errorCalculator.getReport().getError());

            Drawable<ContextType> finalErrorCalcDrawable = resources.createDrawable(finalErrorCalcProgram);
            specularFit.basisResources.useWithShaderProgram(finalErrorCalcProgram);
            finalErrorCalcProgram.setTexture("normalEstimate", specularFit.getNormalMap());
            finalErrorCalcProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
            finalErrorCalcProgram.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
            finalErrorCalcProgram.setUniform("errorGamma", 1.0f);

            rmseOut.println("Final RMSE after diffuse estimate: " +
                runFinalErrorCalculation(finalErrorCalcDrawable, scratchFramebuffer, resources.viewSet.getCameraPoseCount()));

            finalErrorCalcProgram.setUniform("errorGamma", 2.2f);
            rmseOut.println("Final RMSE after diffuse estimate (gamma-corrected): " +
                runFinalErrorCalculation(finalErrorCalcDrawable, scratchFramebuffer, resources.viewSet.getCameraPoseCount()));

            Drawable<ContextType> ggxErrorCalcDrawable = resources.createDrawable(ggxErrorCalcProgram);
            ggxErrorCalcProgram.setTexture("normalEstimate", specularFit.getNormalMap());
            ggxErrorCalcProgram.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
            ggxErrorCalcProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
            ggxErrorCalcProgram.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
            ggxErrorCalcProgram.setUniform("errorGamma", 1.0f);

            rmseOut.println("RMSE for GGX fit: " +
                runFinalErrorCalculation(ggxErrorCalcDrawable, scratchFramebuffer, resources.viewSet.getCameraPoseCount()));

            ggxErrorCalcProgram.setUniform("errorGamma", 2.2f);
            rmseOut.println("RMSE for GGX fit (gamma-corrected): " +
                runFinalErrorCalculation(ggxErrorCalcDrawable, scratchFramebuffer, resources.viewSet.getCameraPoseCount()));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("PackageVisibleField")
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

        for (int k = 0; k < viewCount; k++)
        {
            drawable.program().setUniform("viewIndex", k);

            // Clear framebuffer
            framebuffer.clearDepthBuffer();
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel error.
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Copy framebuffer from GPU to main memory.
            float[] pixelErrors = framebuffer.readFloatingPointColorBufferRGBA(0);

            // Add up per-pixel error.
            WeightedError errorViewTotal = IntStream.range(0, pixelErrors.length / 4)
                .parallel()
                .filter(p -> pixelErrors[4 * p + 3] > 0)
                .collect(() -> new WeightedError(0, 0),
                    (total, p) ->
                    {
                        total.error += pixelErrors[4 * p];
                        total.weight += pixelErrors[4 * p + 3];
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
    Program<ContextType> createFinalErrorCalcProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
            new File("shaders/colorappearance/imgspace_multi_as_single.vert"),
            new File("shaders/specularfit/finalErrorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createGGXErrorCalcProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
            new File("shaders/colorappearance/imgspace_multi_as_single.vert"),
            new File("shaders/specularfit/ggxErrorCalc.frag"),
            false); // Disable visibility and shadow tests for error calculation.
    }

    private void fillHoles(SpecularFitSolution solution)
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
}
