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

package kintsugi3d.builder.fit.normal;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.settings.NormalOptimizationSettings;
import kintsugi3d.builder.resources.project.ReadonlyGraphicsResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.core.*;
import kintsugi3d.optimization.ReadonlyErrorReport;
import kintsugi3d.optimization.ShaderBasedOptimization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class NormalOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(NormalOptimization.class);
    private final ShaderBasedOptimization<ContextType> estimateNormals;
    private final ShaderBasedOptimization<ContextType> smoothNormals;
    private final NormalOptimizationSettings normalOptimizationSettings;

    private boolean firstSmooth = true;

    public NormalOptimization(
        ReadonlyGraphicsResources<ContextType> resources,
        SpecularFitProgramFactory<ContextType> programFactory,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory,
        TextureResolution textureResolution, NormalOptimizationSettings normalOptimizationSettings)
        throws IOException
    {
        this.normalOptimizationSettings = normalOptimizationSettings;

        estimateNormals = new ShaderBasedOptimization<>(
            getNormalEstimationProgramBuilder(resources, programFactory),
            resources.getContext().buildFramebufferObject(textureResolution.width, textureResolution.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true))
                .addColorAttachment(ColorFormat.R32F), // Damping factor while fitting,
            drawableFactory);

        estimateNormals.getFrontFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
        estimateNormals.getBackFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

        smoothNormals = new ShaderBasedOptimization<>(
            getNormalSmoothProgramBuilder(resources, programFactory),
            resources.getContext().buildFramebufferObject(textureResolution.width, textureResolution.height)
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true)),
            drawableFactory);

        smoothNormals.getFrontFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
        smoothNormals.getBackFramebuffer().clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

        estimateNormals.addSetupCallback((estimationProgram, backFramebuffer) ->
        {
            // Update normal estimation program to use the new front buffer.
            estimationProgram.setTexture("normalMap", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(0));
            estimationProgram.setTexture("dampingTex", estimateNormals.getFrontFramebuffer().getColorAttachmentTexture(1));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

            LOG.debug("Estimating normals...");
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

            LOG.debug("Smoothing normals...");
        });

        estimateNormals.addPostUpdateCallback(framebuffer ->
        {
            LOG.debug("DONE!");
        });

        smoothNormals.addPostUpdateCallback(framebuffer ->
        {
            LOG.debug("DONE!");
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

        if (normalOptimizationSettings.getNormalSmoothingIterations() > 0)
        {
            // Copy smoothed result back into normal estimate
            estimateNormals.getFrontFramebuffer().blitColorAttachmentFromFramebuffer(0,
                smoothNormals.getFrontFramebuffer(), 0);
        }
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
            Framebuffer<ContextType> contextTypeFramebuffer = estimateNormals.getFrontFramebuffer();
            contextTypeFramebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, "normal.png"));
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving normal map estimate:", e);
        }
    }

    private ProgramBuilder<ContextType> getNormalEstimationProgramBuilder(
        ReadonlyGraphicsResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
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
        ReadonlyGraphicsResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/texspace_dynamic.vert"),
                new File("shaders/specularfit/smoothNormals.frag"),
                true);
    }
}
