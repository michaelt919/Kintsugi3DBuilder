/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering.components.scene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.state.BackgroundMode;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Matrix4;

public class Environment<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;

    public Environment(ContextType context, SceneViewportModel sceneViewportModel, LightingResources<ContextType> lightingResources,
                       SceneModel sceneModel)
    {
        super(context, sceneViewportModel, "EnvironmentMap");
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        return Map.of("position", context.createRectangle());
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (lightingResources.getEnvironmentMap() != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.ENVIRONMENT_MAP)
        {
            Matrix4 envMapMatrix = sceneModel.getLightingModel().getEnvironmentMapMatrix();

            getDrawable().program().setUniform("useEnvironmentTexture", true);
            getDrawable().program().setTexture("env", lightingResources.getEnvironmentMap());
            getDrawable().program().setUniform("model_view", cameraViewport.getView());
            getDrawable().program().setUniform("projection", cameraViewport.getViewportProjection());
            getDrawable().program().setUniform("envMapMatrix", envMapMatrix);
            getDrawable().program().setUniform("envMapIntensity", sceneModel.getClearColor());

            getDrawable().program().setUniform("gamma",
                lightingResources.getEnvironmentMap().isInternalFormatCompressed() ||
                    lightingResources.getEnvironmentMap().getInternalUncompressedColorFormat().dataType != ColorFormat.DataType.FLOATING_POINT
                    ? 1.0f : 2.2f);

            getContext().getState().disableDepthTest();
            getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            getContext().getState().enableDepthTest();
        }
    }
}
