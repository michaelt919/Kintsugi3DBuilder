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
import java.util.function.Function;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedOptimization;

public class NormalOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final ShaderBasedOptimization<ContextType> estimateNormals;
    private final ShaderBasedOptimization<ContextType> smoothNormals;
    private final SpecularFitSettings settings;

    private boolean firstSmooth = true;

    public NormalOptimization(
        ContextType context,
        SpecularFitProgramFactory<ContextType> programFactory,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory,
        SpecularFitSettings settings)
        throws FileNotFoundException
    {
        this.settings = settings;

        estimateNormals = new ShaderBasedOptimization<>(
            getNormalEstimationProgramBuilder(programFactory),
            context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F), // Damping factor while fitting,
            drawableFactory);

        smoothNormals = new ShaderBasedOptimization<>(
            getNormalSmoothProgramBuilder(programFactory),
            context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true)),
            drawableFactory);

        estimateNormals.addSetupCallback((estimationProgram, backFramebuffer) ->
        {
            // Update normal estimation program to use the new front buffer.
            estimationProgram.setTexture("normalEstimate", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            estimationProgram.setTexture("dampingTex", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(1));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            if (SpecularOptimization.DEBUG)
            {
                System.out.println("Estimating normals...");
            }
        });

        smoothNormals.addSetupCallback((smoothProgram, backFramebuffer) ->
        {
            if (firstSmooth)
            {
                // Use front buffer from original fitting.
                smoothProgram.setTexture("prevNormalEstimate", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            }
            else
            {
                // Update normal smooth program to use the new front buffer.
                smoothProgram.setTexture("prevNormalEstimate", smoothNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            }

            // Pass front buffer from original fitting.
            smoothProgram.setTexture("origNormalEstimate", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            if (SpecularOptimization.DEBUG)
            {
                System.out.println("Smoothing normals...");
            }
        });

        estimateNormals.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                System.out.println("DONE!");
                //saveNormalMapEstimate();
            }
        });

        smoothNormals.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                System.out.println("DONE!");
            }
        });
    }

    @Override
    public void close()
    {
        estimateNormals.close();
        smoothNormals.close();
    }

    public void finish()
    {
        estimateNormals.close();
        smoothNormals.finish();
    }

    public void execute(Function<Texture<ContextType>, ReadonlyErrorReport> errorCalculator, double convergenceTolerance)
    {
        if (settings.isLevenbergMarquardtEnabled())
        {
            // Set damping factor to 1.0 initially at each position.
            estimateNormals.getFrontFramebuffer().clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

            // Estimate using the Levenberg-Marquardt algorithm.
            estimateNormals.runUntilConvergence(errorCalculator, convergenceTolerance, settings.getUnsuccessfulLMIterationsAllowed());
        }
        else
        {
            // Single pass normal estimation.
            estimateNormals.runOnce(errorCalculator);
        }

        if (SpecularOptimization.DEBUG)
        {
            saveNormalMapEstimate();
        }

        firstSmooth = true;
        for (int i = 0; i < settings.getNormalSmoothingIterations(); i++)
        {
            smoothNormals.runOnce();
            firstSmooth = false;
        }

        if (SpecularOptimization.DEBUG)
        {
            saveNormalMap();
        }
    }

    public Texture2D<ContextType> getNormalMap()
    {
        return smoothNormals.getFrontFramebuffer().getColorAttachmentTexture(0);
    }

    public void saveNormalMapEstimate()
    {
        try
        {
            estimateNormals.getFrontFramebuffer().saveColorBufferToFile(0, "PNG",
                new File(settings.outputDirectory, settings.getNormalSmoothingIterations() > 0 ?
                        "normalPreSmooth.png" : "normal.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void saveNormalMap()
    {
        try
        {
            (settings.getNormalSmoothingIterations() > 0 ? smoothNormals : estimateNormals)
                .getFrontFramebuffer().saveColorBufferToFile(0, "PNG",
                    new File(settings.outputDirectory, "normal.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getNormalEstimationProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/specularfit/estimateNormals.frag"),
                true)
            .define("USE_LEVENBERG_MARQUARDT", settings.isLevenbergMarquardtEnabled())
            .define("MIN_DAMPING", settings.getMinNormalDamping());
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getNormalSmoothProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/specularfit/smoothNormals.frag"),
                true);
    }
}
