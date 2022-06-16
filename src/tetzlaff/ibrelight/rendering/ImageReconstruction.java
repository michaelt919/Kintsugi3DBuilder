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

package tetzlaff.ibrelight.rendering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.IntStream;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

public class ImageReconstruction<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final Program<ContextType> program;
    private final Program<ContextType> groundTruthProgram;

    private final Drawable<ContextType> drawable;
    private final Drawable<ContextType> groundTruthDrawable;

    private final FramebufferObject<ContextType> framebuffer;

    public ImageReconstruction(IBRResources<ContextType> resources, ProgramBuilder<ContextType> programBuilder,
                               FramebufferObjectBuilder<ContextType> framebufferObjectBuilder, Consumer<Program<ContextType>> programSetup)
        throws FileNotFoundException
    {
        this.program = programBuilder.createProgram();
        resources.setupShaderProgram(program);
        programSetup.accept(program);
        this.drawable = resources.createDrawable(program);

        this.groundTruthProgram = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/tonemap.frag"))
            .createProgram();
        resources.setupShaderProgram(groundTruthProgram);
        this.groundTruthDrawable = resources.createDrawable(groundTruthProgram);

        this.framebuffer = framebufferObjectBuilder.createFramebufferObject();
    }

    /**
     *
     * @param viewSet
     * @param reconstructionAction
     * @param groundTruthAction
     * @param rmseAction First parameter: view index; second parameter: x-component stores rmse, y-component stores pixel count after masking
     */
    public void execute(ViewSet viewSet,
        BiConsumer<Integer, Framebuffer<ContextType>> reconstructionAction,
        BiConsumer<Integer, Framebuffer<ContextType>> groundTruthAction,
        BiConsumer<Integer, DoubleVector2> rmseAction)
    {
        System.out.println();

        for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
        {
            // Reconstruct the view
            drawable.program().setUniform("model_view", viewSet.getCameraPose(k));
            drawable.program().setUniform("projection",
                viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(k)).getProjectionMatrix(
                    viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
            drawable.program().setUniform("reconstructionCameraPos",
                viewSet.getCameraPoseInverse(k).getColumn(3).getXYZ());
            drawable.program().setUniform("reconstructionLightPos",
                viewSet.getCameraPoseInverse(k).times(viewSet.getLightPosition(viewSet.getLightIndex(k)).asPosition()).getXYZ());
            drawable.program().setUniform("reconstructionLightIntensity",
                    viewSet.getLightIntensity(viewSet.getLightIndex(k)));
            drawable.program().setUniform("gamma", viewSet.getGamma());

            for (int i = 0; i < framebuffer.getColorAttachmentCount(); i++)
            {
                // Clear to black
                framebuffer.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
            }

            // Also clear the depth buffer
            framebuffer.clearDepthBuffer();

            // Draw the view into the framebuffer.
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Give the callback an opportunity to do something with the view.
            if (reconstructionAction != null)
            {
                reconstructionAction.accept(k, framebuffer);
            }

            float[] reconstruction = framebuffer.readFloatingPointColorBufferRGBA(0);

            System.out.println("View " + k + ':');

            // Tonemap ground truth image
            try (Texture2D<ContextType> groundTruthTex =
                groundTruthProgram.getContext().getTextureFactory().build2DColorTextureFromFile(IBRResources.findImageFile(viewSet.getImageFile(k)), true)
                    .setLinearFilteringEnabled(true)
                    .setMipmapsEnabled(true)
                    .createTexture())
            {
                // TODO move into a subroutine to eliminate duplicated code
                groundTruthProgram.setUniform("model_view", viewSet.getCameraPose(k));
                groundTruthProgram.setUniform("projection",
                    viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(k)).getProjectionMatrix(
                        viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                groundTruthProgram.setUniform("reconstructionCameraPos",
                    viewSet.getCameraPoseInverse(k).getColumn(3).getXYZ());
                groundTruthProgram.setUniform("reconstructionLightPos",
                    viewSet.getCameraPoseInverse(k).times(viewSet.getLightPosition(viewSet.getLightIndex(k)).asPosition()).getXYZ());
                groundTruthProgram.setUniform("reconstructionLightIntensity",
                    viewSet.getLightIntensity(viewSet.getLightIndex(k)));
                groundTruthProgram.setTexture("image", groundTruthTex);
                groundTruthProgram.setUniform("gamma", viewSet.getGamma());

                // Clear a single color buffer and depth buffer
                // Use transparent so that we can tell which pixels should be masked
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                framebuffer.clearDepthBuffer();

                // Draw the ground truth image into the framebuffer.
                groundTruthDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

                // Give the callback an opportunity to do something with the view.
                if (groundTruthAction != null)
                {
                    groundTruthAction.accept(k, framebuffer);
                }

                float[] groundTruth = framebuffer.readFloatingPointColorBufferRGBA(0);

                long sampleCount = IntStream.range(0, groundTruth.length / 4)
                    .parallel()
                    .filter(p -> groundTruth[4 * p + 3] > 0.0) // only count pixels where we have geometry (mask out the rest)
                    .count();

                double rmse = Math.sqrt( // root
                    IntStream.range(0, groundTruth.length / 4)
                        .parallel()
                        .filter(p -> groundTruth[4 * p + 3] > 0.0) // only count pixels where we have geometry (mask out the rest)
                        .mapToDouble(p ->
                        {
                            DoubleVector3 groundTruthRGB = new DoubleVector3(groundTruth[4 * p], groundTruth[4 * p + 1], groundTruth[4 * p + 2]);
                            DoubleVector3 reconstructedRGB = new DoubleVector3(reconstruction[4 * p], reconstruction[4 * p + 1], reconstruction[4 * p + 2]);

                            // Handle NaN values -- replace with black
                            reconstructedRGB = reconstructedRGB.applyOperator(x -> Double.isNaN(x) ? 0.0 : x);

                            DoubleVector3 error = groundTruthRGB.minus(reconstructedRGB);
                            return error.dot(error) / 3; // mean squared error for the three channels
                        })
                        .average().orElse(0.0)); // mean over pixels

                if (rmseAction != null)
                {
                    System.out.println("Raw RMSE: " + rmse);

                    // Multiply by the true whitepoint luminance (for an encoded value of 1.0) divided by pi, gamma corrected.
                    // To match the original images dynamic range, the cosine-weighted reflectance values in the framebuffer
                    // will have been pre-divided by this value / PI, then gamma corrected.
                    // Multiplying will reconvert the RMSE back to being in terms of cosine-weighted, normalized reflectance.
                    double decodedWhitePoint = viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);
                    System.out.println("Decoded white point (as reflectance * pi): " + decodedWhitePoint);
                    double normalizedRMSE = rmse * Math.pow(decodedWhitePoint / Math.PI, 1.0 / viewSet.getGamma());
                    rmseAction.accept(k, new DoubleVector2(normalizedRMSE, sampleCount));
                    System.out.println("Normalized RMSE (* white point / pi): " + normalizedRMSE);
                    System.out.println();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close()
    {
        program.close();
        groundTruthProgram.close();
        framebuffer.close();
    }
}
