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

package kintsugi3d.builder.fit.roughness;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.core.*;
import kintsugi3d.optimization.ErrorReport;
import kintsugi3d.optimization.ShaderBasedOptimization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * TODO: sketched out but not fully functional; may not be needed
 * @param <ContextType>
 */
public class RoughnessOptimizationIterative<ContextType extends Context<ContextType>>
        extends RoughnessOptimizationBase<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(RoughnessOptimizationIterative.class);
    private final TextureResolution settings;

    private final ShaderBasedOptimization<ContextType> roughnessOptimization;
//    private Program<ContextType> errorCalcProgram;

    private final double convergenceTolerance;
    private final int unsuccessfulLMIterationsAllowed;

    /**
     *
     * @param basisWeightResources Used for the initial estimate
     * @throws FileNotFoundException
     */
    public RoughnessOptimizationIterative(BasisResources<ContextType> basisResources,
        BasisWeightResources<ContextType> basisWeightResources, TextureResolution settings,
        Supplier<Texture2D<ContextType>> getDiffuseTexture, double convergenceTolerance, int unsuccessfulLMIterationsAllowed)
            throws IOException
    {
        // Inherit from base class to facilitate initial fit.
        super(basisResources);
        setInputWeights(basisWeightResources);
        this.settings = settings;
        this.convergenceTolerance = convergenceTolerance;
        this.unsuccessfulLMIterationsAllowed = unsuccessfulLMIterationsAllowed;

        roughnessOptimization = new ShaderBasedOptimization<>(
            getRoughnessEstimationProgramBuilder(basisResources.getContext()),
            basisResources.getContext().buildFramebufferObject(
                    settings.width, settings.height)
                // Reflectivity map
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true))
                // Roughness map
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                        .setLinearFilteringEnabled(true))
                // Damping factor (R) and error (G) while fitting
                .addColorAttachment(ColorFormat.RG32F),
            program -> // Just use the rectangle as geometry
            {
                Drawable<ContextType> drawable = basisResources.getContext().createDrawable(program);
                drawable.addVertexBuffer("position", rect);
                drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
                return drawable;
            });

//        errorCalcProgram = createErrorCalcProgram(context);
//        Drawable<ContextType> errorCalcDrawable = context.createDrawable(errorCalcProgram);
//        errorCalcDrawable.addVertexBuffer("position", rect);

        roughnessOptimization.addSetupCallback((estimationProgram, backFramebuffer) ->
        {
            // Bind previous estimate textures to the shader
            estimationProgram.setTexture("diffuseMap", getDiffuseTexture.get());
            estimationProgram.setTexture("specularEstimate", getReflectivityTexture()); // front FBO, attachment 0
            estimationProgram.setTexture("roughnessMap", getRoughnessTexture()); // front FBO, attachment 1
            estimationProgram.setTexture("dampingTex", roughnessOptimization.getFrontFramebuffer().getColorAttachmentTexture(2));

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
            backFramebuffer.clearColorBuffer(2, 1.0f /* damping */, Float.MAX_VALUE /* error */, 0.0f, 0.0f);

            log.debug("Optimizing roughness...");
        });
    }

    @Override
    public void close()
    {
        roughnessOptimization.close();
//        errorCalcProgram.close();
    }

    @Override
    protected FramebufferObject<ContextType> getFramebuffer()
    {
        return roughnessOptimization.getFrontFramebuffer();
    }

    @Override
    public void execute(float gamma)
    {
        // Generate initial estimate
        // Renders directly into "front" framebuffer which is fine for the first pass since then we don't have to swap
        super.execute(gamma);

        // Set damping factor to 1.0 initially at each position.
        roughnessOptimization.getFrontFramebuffer().clearColorBuffer(2, 1.0f, 1.0f, 1.0f, 1.0f);

        ErrorReport errorReport = new ErrorReport(settings.width * settings.height);

        // Estimate using the Levenberg-Marquardt algorithm.
        roughnessOptimization.runUntilConvergence(
            framebuffer ->
            {
                float[] dampingError = framebuffer.getTextureReaderForColorAttachment(2).readFloatingPointRGBA();
                errorReport.setError(IntStream.range(0, settings.width * settings.height)
                    .parallel()
                    .mapToDouble(p -> dampingError[4 * p + 1])
                    .sum());
                return errorReport;
            },
            convergenceTolerance, unsuccessfulLMIterationsAllowed);
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getRoughnessEstimationProgramBuilder(
            ContextType context)
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/optimizeRoughness.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/basisToGGXErrorCalc.frag"))
            .createProgram();
    }
}
