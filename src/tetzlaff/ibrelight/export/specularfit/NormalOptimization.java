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

    private final ShaderBasedOptimization<ContextType> base;
    private final SpecularFitSettings settings;

    public NormalOptimization(
        ContextType context,
        SpecularFitProgramFactory<ContextType> programFactory,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory,
        SpecularFitSettings settings)
        throws FileNotFoundException
    {
        base = new ShaderBasedOptimization<>(
            getNormalEstimationProgramBuilder(programFactory),
            context.buildFramebufferObject(settings.width, settings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F).setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F), // Damping factor while fitting,
            drawableFactory);
        this.settings = settings;

        base.addSetupCallback((estimationProgram, backFramebuffer) ->
        {
            // Update normal estimation program to use the new front buffer.
            estimationProgram.setTexture("normalEstimate", base.getFrontFramebuffer().getColorAttachmentTexture(0));
            estimationProgram.setTexture("dampingTex", base.getFrontFramebuffer().getColorAttachmentTexture(1));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
        });

        base.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                System.out.println("DONE!");
                saveNormalMap();
            }
        });
    }

    @Override
    public void close()
    {
        base.close();
    }

    public void finish()
    {
        base.finish();
    }

    public void execute(Function<Texture<ContextType>, ReadonlyErrorReport> errorCalculator, double convergenceTolerance)
    {
        if (USE_LEVENBERG_MARQUARDT)
        {
            // Set damping factor to 1.0 initially at each position.
            base.getFrontFramebuffer().clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

            // Estimate using the Levenberg-Marquardt algorithm.
            base.runUntilConvergence(errorCalculator, convergenceTolerance, UNSUCCESSFUL_ITERATIONS_ALLOWED);
        }
        else
        {
            // Single pass normal estimation.
            base.runOnce(errorCalculator);
        }
    }

    public Texture2D<ContextType> getNormalMap()
    {
        return base.getFrontFramebuffer().getColorAttachmentTexture(0);
    }

    public void saveNormalMap()
    {
        try
        {
            base.getFrontFramebuffer().saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "normal.png"));
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
}
