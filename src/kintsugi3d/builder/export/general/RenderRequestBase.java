/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.general;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

import kintsugi3d.builder.core.ObservableIBRRequest;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.gl.core.*;

abstract class RenderRequestBase implements ObservableIBRRequest
{
    private static final File TEX_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "texspace.vert").toFile();
    private static final File IMG_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "imgspace.vert").toFile();

    private final int width;
    private final int height;
    private final File vertexShader;
    private final File fragmentShader;
    private final ReadonlySettingsModel settingsModel;
    private final Consumer<Program<? extends Context<?>>> shaderSetupCallback;
    private final File outputDirectory;

    RenderRequestBase(int width, int height, ReadonlySettingsModel settingsModel, Consumer<Program<? extends Context<?>>> shaderSetupCallback,
                      File vertexShader, File fragmentShader, File outputDirectory)
    {
        this.width = width;
        this.height = height;
        this.settingsModel = settingsModel;
        this.shaderSetupCallback = shaderSetupCallback;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.outputDirectory = outputDirectory;
    }

    abstract static class BuilderBase implements RenderRequestBuilder
    {
        private final ReadonlySettingsModel settingsModel;
        private final File fragmentShader;
        private final File outputDirectory;

        private int width = 1024;
        private int height = 1024;
        private File vertexShader = TEX_SPACE_VERTEX_SHADER;
        private Consumer<Program<? extends Context<?>>> shaderSetupCallback = null;

        BuilderBase(ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            this.settingsModel = settingsModel;
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

        protected ReadonlySettingsModel getSettingsModel()
        {
            return settingsModel;
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
        IBRResourcesImageSpace<ContextType> resources) throws IOException
    {
        ProgramObject<ContextType> program =
            resources.getShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"))
                .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && this.settingsModel.getBoolean("occlusionEnabled"))
                .define("PHYSICALLY_BASED_MASKING_SHADOWING", this.settingsModel.getBoolean("pbrGeometricAttenuationEnabled"))
                .define("FRESNEL_EFFECT_ENABLED", this.settingsModel.getBoolean("fresnelEnabled"))
                .addShader(ShaderType.VERTEX, vertexShader)
                .addShader(ShaderType.FRAGMENT, fragmentShader)
                .createProgram();

        resources.setupShaderProgram(program);

        program.setUniform("renderGamma", this.settingsModel.getFloat("gamma"));
        program.setUniform("weightExponent", this.settingsModel.getFloat("weightExponent"));
        program.setUniform("isotropyFactor", this.settingsModel.getFloat("isotropyFactor"));
        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

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
        createDrawable(Program<ContextType> program, IBRResourcesImageSpace<ContextType> resources)
    {
        return resources.createDrawable(program);
    }

    protected <ContextType extends Context<ContextType>> void render(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        drawable.getContext().getState().disableBackFaceCulling();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearDepthBuffer();
        shaderSetupCallback.accept(drawable.program());
        drawable.draw(framebuffer);
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }
}
