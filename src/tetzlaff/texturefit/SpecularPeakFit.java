/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.texturefit;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.texturefit.ParameterizedFitBase.SubdivisionRenderingCallback;

public class SpecularPeakFit<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final ParameterizedFitBase<ContextType> base;
    private final Program<ContextType> program;
    private final Consumer<Drawable<ContextType>> shaderSetup;

    private Framebuffer<ContextType> framebuffer;

    SpecularPeakFit(Context<ContextType> context, Consumer<Drawable<ContextType>> shaderSetup, boolean visibilityTest, boolean shadowTest,
        ViewSet viewSet, int subdiv) throws IOException
    {
        this.program = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit", "specularpeakfit_imgspace.frag").toFile())
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", viewSet.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", visibilityTest)
            .define("SHADOW_TEST_ENABLED", shadowTest)
            .createProgram();

        this.shaderSetup = shaderSetup;

        this.base = new ParameterizedFitBase<>(context.createDrawable(this.program), viewSet.getCameraPoseCount(), subdiv);
    }

    @Override
    public void close()
    {
        this.program.close();
    }

    void setFramebuffer(Framebuffer<ContextType> framebuffer)
    {
        this.framebuffer = framebuffer;
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages,
        Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> peakEstimate,
        SubdivisionRenderingCallback callback) throws IOException
    {
        shaderSetup.accept(this.base.drawable);
        program.setTexture("diffuseEstimate", diffuseEstimate);
        program.setTexture("normalEstimate", normalEstimate);
        program.setTexture("peakEstimate", peakEstimate);
        base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
    }
}
