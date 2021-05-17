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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;

import javax.imageio.ImageIO;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;

public class SpecularFitFinalizer
{
    private final SpecularFitSettings settings;

    public SpecularFitFinalizer(SpecularFitSettings settings)
    {
        this.settings = settings;
    }

    public <ContextType extends Context<ContextType>> void finalize(
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
            fillHoles(solution);

            // Save the weight map and preliminary diffuse result after filling holes
            saveWeightMaps(solution);
            saveDiffuseMap(solution);

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
            saveBasisFunctions(solution, basisImageDrawable, basisImageFramebuffer);

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

            reconstructImages(imageReconstructionDrawable, imageReconstructionFramebuffer, resources.viewSet, "reconstructions");

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

            reconstructImages(fittedImageReconstructionDrawable, imageReconstructionFramebuffer, resources.viewSet, "fitted");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
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

    private <ContextType extends Context<ContextType>> void saveBasisFunctions(
        SpecularFitSolution solution, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
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

    private void saveWeightMaps(SpecularFitSolution solution)
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

    private void saveDiffuseMap(SpecularFitSolution solution)
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

    private <ContextType extends Context<ContextType>> void reconstructImages(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, ViewSet viewSet, String folderName)
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
