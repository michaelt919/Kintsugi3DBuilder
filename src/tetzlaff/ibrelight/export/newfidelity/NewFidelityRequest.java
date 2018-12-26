/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.newfidelity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.ejml.data.FMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.lwjgl.*;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.NonNegativeLeastSquaresSinglePrecision;

import static org.ejml.dense.row.CommonOps_FDRM.multTransA;

public class NewFidelityRequest implements IBRRequest
{
    private final int renderWidth;
    private final int renderHeight;
    private final File targetVSETFile;
    private final File debugPath;
    private final ReadonlySettingsModel settings;

    private static final boolean DEBUG = true;
    
    public NewFidelityRequest(int renderWidth, int renderHeight, File targetVSETFile, File debugPath, ReadonlySettingsModel settings)
    {
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        this.targetVSETFile = targetVSETFile;
        this.debugPath = debugPath;
        this.settings = settings;
    }

    public <ContextType extends Context<ContextType>> double evaluateError(
            IBRResources<ContextType> resources, Matrix4 targetModelView, Matrix4 targetProjection, File targetImageFile)
        throws IOException
    {
        BufferedImage targetImage = ImageIO.read(targetImageFile);
        int width = targetImage.getWidth();
        int height = targetImage.getHeight();

        SimpleMatrix redTarget = new SimpleMatrix(width * height, 1, FMatrixRMaj.class);
        SimpleMatrix greenTarget = new SimpleMatrix(width * height, 1, FMatrixRMaj.class);
        SimpleMatrix blueTarget = new SimpleMatrix(width * height, 1, FMatrixRMaj.class);

        int k = 0;
        for (int y = targetImage.getHeight() - 1; y >= 0; y--)
        {
            for (int x = 0; x < targetImage.getWidth(); x++)
            {
                Color rgb = new Color(targetImage.getRGB(x, y));
                redTarget.set(k, Math.pow(rgb.getRed() / 255.0, 2.2));
                greenTarget.set(k, Math.pow(rgb.getGreen() / 255.0, 2.2));
                blueTarget.set(k, Math.pow(rgb.getBlue() / 255.0, 2.2));
                k++;
            }
        }

        try
        (
            Program<ContextType> mainProgram = resources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.getBoolean("occlusionEnabled"))
                .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.getBoolean("occlusionEnabled"))
                .define("FRESNEL_EFFECT_ENABLED", true)
                .define("SHADOWS_ENABLED", true)
                .define("RAY_DEPTH_GRADIENT", 0.2 * resources.geometry.getBoundingRadius())
                .define("RAY_POSITION_JITTER", 0.02 * resources.geometry.getBoundingRadius())
                .define("MAX_RAYTRACING_SAMPLE_COUNT", 64)
                .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/transformViewpoint.frag"))
                .createProgram();

            Program<ContextType> shadowProgram = resources.context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "depth.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "depth.frag"))
                    .createProgram();

            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(renderWidth, renderHeight)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment()
                .createFramebufferObject();

            FramebufferObject<ContextType> shadowFramebuffer = resources.context.buildFramebufferObject(renderWidth, renderHeight)
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> shadowDrawable = resources.context.createDrawable(shadowProgram);
            shadowDrawable.addVertexBuffer("position", resources.positionBuffer);
            shadowProgram.setUniform("model_view", targetModelView);
            shadowProgram.setUniform("projection", targetProjection);

            shadowFramebuffer.clearDepthBuffer();
            shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);

            Drawable<ContextType> mainDrawable = resources.context.createDrawable(mainProgram);
            mainDrawable.addVertexBuffer("position", resources.positionBuffer);
            mainDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            mainDrawable.addVertexBuffer("normal", resources.normalBuffer);
            mainDrawable.addVertexBuffer("tangent", resources.tangentBuffer);

            resources.setupShaderProgram(mainProgram);

            mainProgram.setTexture("screenSpaceDepthBuffer", shadowFramebuffer.getDepthAttachmentTexture());

            if (settings.getBoolean("occlusionEnabled"))
            {
                mainProgram.setUniform("occlusionBias", settings.getFloat("occlusionBias"));
            }

            mainProgram.setUniform("model_view", targetModelView);
            mainProgram.setUniform("projection", targetProjection);
            mainProgram.setUniform("fullProjection", targetProjection);

            FloatBuffer colorFloatBuffer = BufferUtils.createFloatBuffer(renderWidth * renderHeight * 4);

            float[] colorSums = new float[width * height * 3];
            int[] colorCounts = new int[width * height];

            SimpleMatrix red = new SimpleMatrix(width * height, resources.viewSet.getCameraPoseCount(), FMatrixRMaj.class);
            SimpleMatrix green = new SimpleMatrix(width * height, resources.viewSet.getCameraPoseCount(), FMatrixRMaj.class);
            SimpleMatrix blue = new SimpleMatrix(width * height, resources.viewSet.getCameraPoseCount(), FMatrixRMaj.class);

            for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
            {
                framebuffer.clearDepthBuffer();
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

                mainProgram.setUniform("viewIndex", i);

                mainDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

                framebuffer.readFloatingPointColorBufferRGBA(0, colorFloatBuffer);
                Arrays.fill(colorSums, 0);
                Arrays.fill(colorCounts, 0);

                for (int j = 0; j < renderWidth * renderHeight; j++)
                {
                    if (colorFloatBuffer.get(4 * j + 3) > 0)
                    {
                        Vector3 colorVector =
                            new Vector3(colorFloatBuffer.get(4 * j), colorFloatBuffer.get(4 * j + 1), colorFloatBuffer.get(4 * j + 2));

                        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
                        int j2 = (int)Math.floor((j % renderWidth) * width / (double)renderWidth)
                            + width * (int)Math.floor((j / renderWidth) * height / (double)renderHeight);

                        colorSums[3 * j2]     += colorVector.x;
                        colorSums[3 * j2 + 1] += colorVector.y;
                        colorSums[3 * j2 + 2] += colorVector.z;
                        colorCounts[j2]++;
                    }
                }

                for (int j = 0; j < colorCounts.length; j++)
                {
                    if (colorCounts[j] > 0)
                    {
                        red.set(j, i, colorSums[3 * j] / (float) colorCounts[j]);
                        green.set(j, i, colorSums[3 * j + 1] / (float) colorCounts[j]);
                        blue.set(j, i, colorSums[3 * j + 2] / (float) colorCounts[j]);
                    }
                    else
                    {
                        redTarget.set(j, 0.0);
                        greenTarget.set(j, 0.0);
                        blueTarget.set(j, 0.0);
                    }
                }

                if (DEBUG)
                {
                    try
                    {
                        String[] filenameParts = resources.viewSet.getImageFileName(i).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";

                        framebuffer.saveColorBufferToFile(0, "PNG",
                            new File(debugPath, String.join(".", filenameParts)));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            SimpleMatrix redATA = new SimpleMatrix(red.numCols(), red.numCols(), FMatrixRMaj.class);
            SimpleMatrix greenATA = new SimpleMatrix(red.numCols(), red.numCols(), FMatrixRMaj.class);
            SimpleMatrix blueATA = new SimpleMatrix(red.numCols(), red.numCols(), FMatrixRMaj.class);

            SimpleMatrix redATb = new SimpleMatrix(red.numCols(), 1, FMatrixRMaj.class);
            SimpleMatrix greenATb = new SimpleMatrix(red.numCols(), 1, FMatrixRMaj.class);
            SimpleMatrix blueATb = new SimpleMatrix(red.numCols(), 1, FMatrixRMaj.class);

            // Low level operations to avoid using unnecessary memory.
            multTransA(red.getMatrix(), red.getMatrix(), redATA.getMatrix());
            multTransA(green.getMatrix(), green.getMatrix(), greenATA.getMatrix());
            multTransA(blue.getMatrix(), blue.getMatrix(), blueATA.getMatrix());

            multTransA(red.getMatrix(), redTarget.getMatrix(), redATb.getMatrix());
            multTransA(green.getMatrix(), greenTarget.getMatrix(), greenATb.getMatrix());
            multTransA(blue.getMatrix(), blueTarget.getMatrix(), blueATb.getMatrix());

            NonNegativeLeastSquaresSinglePrecision solver = new NonNegativeLeastSquaresSinglePrecision();

            SimpleMatrix redSolution = solver.solvePremultiplied(redATA, redATb, 0.001);
            SimpleMatrix redRecon = red.mult(redSolution);

            SimpleMatrix greenSolution = solver.solvePremultiplied(greenATA, greenATb, 0.001);
            SimpleMatrix greenRecon = green.mult(greenSolution);

            SimpleMatrix blueSolution = solver.solvePremultiplied(blueATA, blueATb, 0.001);
            SimpleMatrix blueRecon = blue.mult(blueSolution);

            String[] fileNameParts = targetImageFile.getName().split("\\.");
            fileNameParts[fileNameParts.length - 1] = "png";
            String debugFileName = String.join(".", fileNameParts);

            if (DEBUG)
            {
                BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                int[] pixels = IntStream.range(0, width * height).map(i -> new Color(0, 0, 0).getRGB()).toArray();

                for (int i = 0; i < width * height; i++)
                {
                    pixels[i] = new Color(
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)redRecon.get(i), 1.0 / 2.2)))),
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)greenRecon.get(i), 1.0 / 2.2)))),
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)blueRecon.get(i), 1.0 / 2.2)))))
                        .getRGB();
                }

                // Flip the array vertically
                for (int y = 0; y < height / 2; y++)
                {
                    int limit = (y + 1) * width;
                    for (int i1 = y * width, i2 = (height - y - 1) * width; i1 < limit; i1++, i2++)
                    {
                        int tmp = pixels[i1];
                        pixels[i1] = pixels[i2];
                        pixels[i2] = tmp;
                    }
                }

                outImg.setRGB(0, 0, width, height, pixels, 0, width);

                try
                {
                    ImageIO.write(outImg, "PNG", new File(debugPath, "recon_" + debugFileName));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            SimpleMatrix redError = redRecon.minus(redTarget);
            SimpleMatrix greenError = redRecon.minus(greenTarget);
            SimpleMatrix blueError = redRecon.minus(blueTarget);

            if (DEBUG)
            {
                BufferedImage errorImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                int[] errorPixels = IntStream.range(0, width * height).map(i -> new Color(0, 0, 0).getRGB()).toArray();

                for (int i = 0; i < width * height; i++)
                {
                    errorPixels[i] = new Color(
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)Math.abs(redError.get(i)), 1.0 / 2.2)))),
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)Math.abs(greenError.get(i)), 1.0 / 2.2)))),
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)Math.abs(blueError.get(i)), 1.0 / 2.2)))))
                        .getRGB();
                }

                // Flip the array vertically
                for (int y = 0; y < height / 2; y++)
                {
                    int limit = (y + 1) * width;
                    for (int i1 = y * width, i2 = (height - y - 1) * width; i1 < limit; i1++, i2++)
                    {
                        int tmp = errorPixels[i1];
                        errorPixels[i1] = errorPixels[i2];
                        errorPixels[i2] = tmp;
                    }
                }

                errorImg.setRGB(0, 0, width, height, errorPixels, 0, width);

                try
                {
                    ImageIO.write(errorImg, "PNG", new File(debugPath, "error_" + debugFileName));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                BufferedImage originalImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                int[] originalPixels = IntStream.range(0, width * height).map(i -> new Color(0, 0, 0).getRGB()).toArray();

                for (int i = 0; i < width * height; i++)
                {
                    originalPixels[i] = new Color(
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)redTarget.get(i), 1.0 / 2.2)))),
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)greenTarget.get(i), 1.0 / 2.2)))),
                        Math.max(0, Math.min(255, (int)Math.round(255.0 * Math.pow((float)blueTarget.get(i), 1.0 / 2.2)))))
                        .getRGB();
                }

                // Flip the array vertically
                for (int y = 0; y < height / 2; y++)
                {
                    int limit = (y + 1) * width;
                    for (int i1 = y * width, i2 = (height - y - 1) * width; i1 < limit; i1++, i2++)
                    {
                        int tmp = originalPixels[i1];
                        originalPixels[i1] = originalPixels[i2];
                        originalPixels[i2] = tmp;
                    }
                }

                originalImg.setRGB(0, 0, width, height, originalPixels, 0, width);

                try
                {
                    ImageIO.write(originalImg, "PNG", new File(debugPath, "original_" + debugFileName));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            int envMapHeight = (int)Math.ceil(4 * Math.sqrt(Math.PI * resources.viewSet.getCameraPoseCount()));
            int envMapWidth = 2 * envMapHeight;

            BufferedImage envMapImg = new BufferedImage(envMapWidth, envMapHeight, BufferedImage.TYPE_INT_ARGB);

//            double maxWeight = Math.max(
//                IntStream.range(0, redSolution.getNumElements()).mapToDouble(redSolution::get).max().orElse(0.0),
//                Math.max(
//                    IntStream.range(0, greenSolution.getNumElements()).mapToDouble(greenSolution::get).max().orElse(0.0),
//                    IntStream.range(0, blueSolution.getNumElements()).mapToDouble(blueSolution::get).max().orElse(0.0)));

//            double scale = Math.max(1.0, maxWeight);

            double scale = Math.max(1.0,
                IntStream.range(0, greenSolution.getNumElements())
                    .mapToDouble(greenSolution::get)
                    .filter(x -> x > 0.00001)
                    .average().orElse(0.5) * 2);

//            double scale = 1.0;

            for (int x = 0; x < envMapWidth; x++)
            {
                for (int y = 0; y < envMapHeight; y++)
                {
                    int nearestView = -1;
                    double nearestDistance = Double.MAX_VALUE;

                    for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
                    {
                        Vector3 halfDir = targetModelView
                            .times(resources.viewSet.getCameraPoseInverse(i)
                                .times(resources.viewSet.getLightPosition(resources.viewSet.getLightIndex(i)).times(0.5f).asPosition())
                                .minus(resources.geometry.getCentroid().asPosition()))
                            .getXYZ().normalized();

                        Vector3 viewDir = targetModelView.times(resources.geometry.getCentroid().asPosition()).getXYZ().negated().normalized();
                        Vector3 lightDir = halfDir.times(2 * halfDir.dot(viewDir)).minus(viewDir);

                        double theta = x * 2 * Math.PI / envMapWidth;
                        double phi = y * Math.PI / (envMapHeight - 1);

                        Vector3 targetLightDir =
                            new Vector3((float)(Math.sin(theta) * Math.sin(phi)), (float)Math.cos(phi), (float)(-Math.cos(theta) * Math.sin(phi)));

                        double distance = targetLightDir.distance(lightDir);

                        if (distance < nearestDistance)
                        {
                            nearestDistance = distance;
                            nearestView = i;
                        }
                    }

                    if (nearestView != -1)
                    {
                        Color rgb = new Color(
                            Math.max(0, Math.min(1, Math.round(Math.pow(redSolution.get(nearestView) / scale, 1.0 / 2.2)))),
                            Math.max(0, Math.min(1, Math.round(Math.pow(greenSolution.get(nearestView) / scale, 1.0 / 2.2)))),
                            Math.max(0, Math.min(1, Math.round(Math.pow(blueSolution.get(nearestView) / scale, 1.0 / 2.2)))));

                        envMapImg.setRGB(x, y, rgb.getRGB());
                    }
                }
            }

            try
            {
                ImageIO.write(envMapImg, "PNG", new File(debugPath, "environment.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            double sqError = (redError.dot(redError) + greenError.dot(greenError) + blueError.dot(blueError))
                / (redTarget.dot(redTarget) + greenTarget.dot(greenTarget) + blueTarget.dot(blueTarget));

            try(PrintStream info = new PrintStream(new File(debugPath, "info.txt")))
            {
                info.println("Fidelity:\t" + Math.sqrt(1.0 - sqError));
                info.println();

                for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
                {
                    info.println(resources.viewSet.getImageFileName(i)
                        + '\t' + redSolution.get(i) + '\t' + greenSolution.get(i) + '\t' + blueSolution.get(i));
                }
            }

            return sqError;
        }
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        ViewSet targetViewSet = ViewSet.loadFromVSETFile(targetVSETFile);

        double error = evaluateError(renderable.getResources(),
            targetViewSet.getCameraPose(targetViewSet.getPrimaryViewIndex()),
                targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(targetViewSet.getPrimaryViewIndex()))
                    .getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()),
                targetViewSet.getImageFile(targetViewSet.getPrimaryViewIndex()));

        System.out.println("Fidelity: " + Math.sqrt(1.0 - error));
    }
}
