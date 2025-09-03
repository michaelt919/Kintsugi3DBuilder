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

package kintsugi3d.builder.export.general;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.state.GlobalSettingsModel;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

abstract class RenderRequestBase implements ObservableProjectGraphicsRequest
{
    private static final File TEX_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "texspace.vert").toFile();
    private static final File IMG_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "imgspace.vert").toFile();

    private final int width;
    private final int height;
    private final File vertexShader;
    private final File fragmentShader;
    private final Consumer<Program<? extends Context<?>>> shaderSetupCallback;
    private final File outputDirectory;

    RenderRequestBase(int width, int height, Consumer<Program<? extends Context<?>>> shaderSetupCallback,
                      File vertexShader, File fragmentShader, File outputDirectory)
    {
        this.width = width;
        this.height = height;
        this.shaderSetupCallback = shaderSetupCallback;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.outputDirectory = outputDirectory;
    }

    abstract static class BuilderBase implements RenderRequestBuilder
    {
        private final File fragmentShader;
        private final File outputDirectory;

        private int width = 1024;
        private int height = 1024;
        private File vertexShader = TEX_SPACE_VERTEX_SHADER;
        private Consumer<Program<? extends Context<?>>> shaderSetupCallback = null;

        BuilderBase( File fragmentShader, File outputDirectory)
        {
            this.fragmentShader = fragmentShader;
            this.outputDirectory = outputDirectory;
        }

        protected int getWidth()
        {
            return width;
        }

        protected int getHeight()
        {
            return height;
        }

        protected Consumer<Program<? extends Context<?>>> getShaderSetupCallback()
        {
            return shaderSetupCallback;
        }

        protected File getVertexShader()
        {
            return vertexShader;
        }

        protected File getFragmentShader()
        {
            return fragmentShader;
        }

        protected File getOutputDirectory()
        {
            return outputDirectory;
        }

        @Override
        public RenderRequestBuilder useTextureSpaceVertexShader()
        {
            this.vertexShader = TEX_SPACE_VERTEX_SHADER;
            return this;
        }

        @Override
        public RenderRequestBuilder useCameraSpaceVertexShader()
        {
            this.vertexShader = IMG_SPACE_VERTEX_SHADER;
            return this;
        }

        @Override
        public RenderRequestBuilder useCustomVertexShader(File vertexShader)
        {
            this.vertexShader = vertexShader;
            return this;
        }

        @Override
        public RenderRequestBuilder setShaderSetupCallback(Consumer<Program<? extends Context<?>>> shaderSetupCallback)
        {
            this.shaderSetupCallback = shaderSetupCallback;
            return this;
        }

        @Override
        public RenderRequestBuilder setWidth(int width)
        {
            this.width = width;
            return this;
        }

        @Override
        public RenderRequestBuilder setHeight(int height)
        {
            this.height = height;
            return this;
        }
    }

    protected <ContextType extends Context<ContextType>> ProgramObject<ContextType> createProgram(
        GraphicsResourcesImageSpace<ContextType> resources) throws IOException
    {
        GlobalSettingsModel settingsModel = Global.state().getSettingsModel();

        ProgramObject<ContextType> program =
            resources.getShaderProgramBuilder()
                .define("PHYSICALLY_BASED_MASKING_SHADOWING", settingsModel.getBoolean("pbrGeometricAttenuationEnabled"))
                .define("FRESNEL_EFFECT_ENABLED", settingsModel.getBoolean("fresnelEnabled"))
                .addShader(ShaderType.VERTEX, vertexShader)
                .addShader(ShaderType.FRAGMENT, fragmentShader)
                .createProgram();

        resources.setupShaderProgram(program);

        program.setUniform("weightExponent", settingsModel.getFloat("weightExponent"));
        program.setUniform("isotropyFactor", settingsModel.getFloat("isotropyFactor"));

        return program;
    }

    protected <ContextType extends Context<ContextType>> FramebufferObject<ContextType> createFramebuffer(ContextType context)
    {
        return context.buildFramebufferObject(width, height)
            .addColorAttachment()
            .addDepthAttachment()
            .createFramebufferObject();
    }

    protected static <ContextType extends Context<ContextType>> Drawable<ContextType>
        createDrawable(Program<ContextType> program, GraphicsResourcesImageSpace<ContextType> resources)
    {
        return resources.createDrawable(program);
    }

    protected <ContextType extends Context<ContextType>> void render(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        drawable.getContext().getState().disableBackFaceCulling();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearDepthBuffer();
        if(shaderSetupCallback != null){
            shaderSetupCallback.accept(drawable.program());
        }
        drawable.draw(framebuffer);
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }
}
