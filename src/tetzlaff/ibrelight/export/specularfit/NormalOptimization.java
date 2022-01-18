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
    private static final boolean USE_LEVENBERG_MARQUARDT = !SpecularOptimization.ORIGINAL_NAM_METHOD;
    private static final int UNSUCCESSFUL_ITERATIONS_ALLOWED = 8;

    private static final int CLEAN_ITERATIONS_FACTOR = 32;

    private final ShaderBasedOptimization<ContextType> estimateNormals;
    private final ShaderBasedOptimization<ContextType> cleanNormals;
    private final SpecularFitSettings settings;

    private boolean firstClean = true;

    public NormalOptimization(
        ContextType context,
        SpecularFitProgramFactory<ContextType> programFactory,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory,
        SpecularFitSettings settings)
        throws FileNotFoundException
    {
        estimateNormals = new ShaderBasedOptimization<>(
            getNormalEstimationProgramBuilder(programFactory),
            context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F), // Damping factor while fitting,
            drawableFactory);

        cleanNormals = new ShaderBasedOptimization<>(
            getNormalCleanProgramBuilder(programFactory),
            context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true)),
            drawableFactory);

        this.settings = settings;

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

        cleanNormals.addSetupCallback((cleanProgram, backFramebuffer) ->
        {
            if (firstClean)
            {
                // Use front buffer from original fitting.
                cleanProgram.setTexture("prevNormalEstimate", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            }
            else
            {
                // Update normal clean program to use the new front buffer.
                cleanProgram.setTexture("prevNormalEstimate", cleanNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            }

            // Pass front buffer from original fitting.
            cleanProgram.setTexture("origNormalEstimate", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            if (SpecularOptimization.DEBUG)
            {
                System.out.println("Cleaning normals...");
            }
        });

        estimateNormals.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                System.out.println("DONE!");
                saveNormalMapEstimate();
            }
        });

        cleanNormals.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                System.out.println("DONE!");
//                saveNormalMap();
            }
        });
    }

    @Override
    public void close()
    {
        estimateNormals.close();
        cleanNormals.close();
    }

    public void finish()
    {
        estimateNormals.close();
        cleanNormals.finish();
    }

    public void execute(Function<Texture<ContextType>, ReadonlyErrorReport> errorCalculator, double convergenceTolerance)
    {
        if (USE_LEVENBERG_MARQUARDT)
        {
            // Set damping factor to 1.0 initially at each position.
            estimateNormals.getFrontFramebuffer().clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

            // Estimate using the Levenberg-Marquardt algorithm.
            estimateNormals.runUntilConvergence(errorCalculator, convergenceTolerance, UNSUCCESSFUL_ITERATIONS_ALLOWED);
        }
        else
        {
            // Single pass normal estimation.
            estimateNormals.runOnce(errorCalculator);
        }

        firstClean = true;
        int cleanIterations = Math.max(settings.width, settings.height) / (2 * CLEAN_ITERATIONS_FACTOR);
        for (int i = 0; i < cleanIterations; i++)
        {
            cleanNormals.runOnce();
            firstClean = false;
        }
        saveNormalMap();
    }

    public Texture2D<ContextType> getNormalMap()
    {
        return cleanNormals.getFrontFramebuffer().getColorAttachmentTexture(0);
    }

    public void saveNormalMapEstimate()
    {
        try
        {
            estimateNormals.getFrontFramebuffer().saveColorBufferToFile(0, "PNG",
                new File(settings.outputDirectory, "normalPreClean.png"));
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
            cleanNormals.getFrontFramebuffer().saveColorBufferToFile(0, "PNG",
                new File(settings.outputDirectory, "normal.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getNormalEstimationProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/specularfit/estimateNormals.frag"),
                true)
            .define("USE_LEVENBERG_MARQUARDT", USE_LEVENBERG_MARQUARDT);
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getNormalCleanProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/specularfit/cleanNormals.frag"),
                true);
    }
}
