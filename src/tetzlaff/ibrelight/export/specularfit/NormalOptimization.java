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
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.export.specularfit.settings.NormalOptimizationSettings;
import tetzlaff.ibrelight.rendering.resources.ReadonlyIBRResources;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedOptimization;

public class NormalOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(NormalOptimization.class);
    private final ShaderBasedOptimization<ContextType> estimateNormals;
    private final ShaderBasedOptimization<ContextType> smoothNormals;
    private final NormalOptimizationSettings normalOptimizationSettings;

    private boolean firstSmooth = true;

    public NormalOptimization(
        ReadonlyIBRResources<ContextType> resources,
        SpecularFitProgramFactory<ContextType> programFactory,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory,
        TextureFitSettings textureFitSettings, NormalOptimizationSettings normalOptimizationSettings)
        throws FileNotFoundException
    {
        this.normalOptimizationSettings = normalOptimizationSettings;

        estimateNormals = new ShaderBasedOptimization<>(
            getNormalEstimationProgramBuilder(resources, programFactory),
            resources.getContext().buildFramebufferObject(textureFitSettings.width, textureFitSettings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F), // Damping factor while fitting,
            drawableFactory);

        estimateNormals.getFrontFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
        estimateNormals.getBackFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

        smoothNormals = new ShaderBasedOptimization<>(
            getNormalSmoothProgramBuilder(resources, programFactory),
            resources.getContext().buildFramebufferObject(textureFitSettings.width, textureFitSettings.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true)),
            drawableFactory);

        smoothNormals.getFrontFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
        smoothNormals.getBackFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

        estimateNormals.addSetupCallback((estimationProgram, backFramebuffer) ->
        {
            // Update normal estimation program to use the new front buffer.
            estimationProgram.setTexture("normalEstimate", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            estimationProgram.setTexture("dampingTex", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(1));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            if (SpecularOptimization.DEBUG)
            {
                log.info("Estimating normals...");
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
                log.info("Smoothing normals...");
            }
        });

        estimateNormals.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                log.info("DONE!");
                //saveNormalMapEstimate();
            }
        });

        smoothNormals.addPostUpdateCallback(framebuffer ->
        {
            if (SpecularOptimization.DEBUG)
            {
                log.info("DONE!");
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

    public boolean isNormalRefinementEnabled()
    {
        return normalOptimizationSettings.isNormalRefinementEnabled();
    }

    public void execute(Function<Texture<ContextType>, ReadonlyErrorReport> errorCalculator, double convergenceTolerance)
    {
        if (normalOptimizationSettings.isLevenbergMarquardtEnabled())
        {
            // Set damping factor to 1.0 initially at each position.
            estimateNormals.getFrontFramebuffer().clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

            // Estimate using the Levenberg-Marquardt algorithm.
            estimateNormals.runUntilConvergence(framebuffer -> errorCalculator.apply(framebuffer.getColorAttachmentTexture(0)),
                    convergenceTolerance, normalOptimizationSettings.getUnsuccessfulLMIterationsAllowed());
        }
        else
        {
            // Single pass normal estimation.
            // Accept results regardless of whether they make the error better or not.
            // (Primarily to be used for comparison with Levenberg-Marquardt, probably not useful in practice).
            estimateNormals.runOnce();
        }

//        if (SpecularOptimization.DEBUG)
//        {
//            saveNormalMapEstimate();
//        }

        firstSmooth = true;
        for (int i = 0; i < normalOptimizationSettings.getNormalSmoothingIterations(); i++)
        {
            smoothNormals.runOnce();
            firstSmooth = false;
        }

        // Copy smoothed result back into normal estimate
        estimateNormals.getFrontFramebuffer().blitColorAttachmentFromFramebuffer(0,
            smoothNormals.getFrontFramebuffer(), 0);
    }

    private FramebufferObject<ContextType> getNormalMapFBO()
    {
        return estimateNormals.getFrontFramebuffer();
    }


    public Texture2D<ContextType> getNormalMap()
    {
        return getNormalMapFBO().getColorAttachmentTexture(0);
    }

    public void saveNormalMapEstimate(File outputDirectory)
    {
        try
        {
            estimateNormals.getFrontFramebuffer().saveColorBufferToFile(0, "PNG",
                new File(outputDirectory, "normal.png"));
        }
        catch (IOException e)
        {
            log.error("An error occurred saving normal map estimate:", e);
        }
    }

    private ProgramBuilder<ContextType> getNormalEstimationProgramBuilder(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/texspace_dynamic.vert"),
                new File("shaders/specularfit/estimateNormals.frag"),
                true)
            .define("USE_LEVENBERG_MARQUARDT", normalOptimizationSettings.isLevenbergMarquardtEnabled())
            .define("MIN_DAMPING", normalOptimizationSettings.getMinNormalDamping());
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getNormalSmoothProgramBuilder(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/texspace_dynamic.vert"),
                new File("shaders/specularfit/smoothNormals.frag"),
                true);
    }
}
