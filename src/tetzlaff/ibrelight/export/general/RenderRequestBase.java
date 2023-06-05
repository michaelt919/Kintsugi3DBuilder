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

package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.function.Consumer;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

abstract class RenderRequestBase<ContextType extends Context<ContextType>> implements IBRRequest<ContextType>
{
    private static final File TEX_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "texspace_noscale.vert").toFile();
    private static final File IMG_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "imgspace.vert").toFile();

    private final int width;
    private final int height;
    private final File vertexShader;
    private final File fragmentShader;
    private final ReadonlySettingsModel settingsModel;
    private final Consumer<Program<ContextType>> shaderSetupCallback;
    private final File outputDirectory;

    RenderRequestBase(int width, int height, ReadonlySettingsModel settingsModel, Consumer<Program<ContextType>> shaderSetupCallback,
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

    abstract static class BuilderBase<ContextType extends Context<ContextType>> implements RenderRequestBuilder<ContextType>
    {
        private final ReadonlySettingsModel settingsModel;
        private final File fragmentShader;
        private final File outputDirectory;

        private int width = 1024;
        private int height = 1024;
        private File vertexShader = TEX_SPACE_VERTEX_SHADER;
        private Consumer<Program<ContextType>> shaderSetupCallback = null;

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

        protected Consumer<Program<ContextType>> getShaderSetupCallback()
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
        public RenderRequestBuilder<ContextType> useTextureSpaceVertexShader()
        {
            this.vertexShader = TEX_SPACE_VERTEX_SHADER;
            return this;
        }

        @Override
        public RenderRequestBuilder<ContextType> useCameraSpaceVertexShader()
        {
            this.vertexShader = IMG_SPACE_VERTEX_SHADER;
            return this;
        }

        @Override
        public RenderRequestBuilder<ContextType> useCustomVertexShader(File vertexShader)
        {
            this.vertexShader = vertexShader;
            return this;
        }

        @Override
        public RenderRequestBuilder<ContextType> setShaderSetupCallback(Consumer<Program<ContextType>> shaderSetupCallback)
        {
            this.shaderSetupCallback = shaderSetupCallback;
            return this;
        }

        @Override
        public RenderRequestBuilder<ContextType> setWidth(int width)
        {
            this.width = width;
            return this;
        }

        @Override
        public RenderRequestBuilder<ContextType> setHeight(int height)
        {
            this.height = height;
            return this;
        }
    }

    protected
    Program<ContextType> createProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program =
            resources.getIBRShaderProgramBuilder(this.settingsModel.get("renderingMode", StandardRenderingMode.class))
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

    protected FramebufferObject<ContextType> createFramebuffer(ContextType context)
    {
        return context.buildFramebufferObject(width, height)
            .addColorAttachment()
            .addDepthAttachment()
            .createFramebufferObject();
    }

    protected static <ContextType extends Context<ContextType>> Drawable<ContextType>
        createDrawable(Program<ContextType> program, IBRResources<ContextType> resources)
    {
        return resources.createDrawable(program);
    }

    protected void render(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        drawable.getContext().getState().disableBackFaceCulling();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearDepthBuffer();
        shaderSetupCallback.accept(drawable.program());
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }
}
