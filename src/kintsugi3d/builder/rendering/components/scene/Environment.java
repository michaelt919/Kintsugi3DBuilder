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

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.state.BackgroundMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class Environment<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(Environment.class);
    private final ContextType context;
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private ProgramObject<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;
    private VertexBuffer<ContextType> rectangleVertices;

    public Environment(ContextType context, LightingResources<ContextType> lightingResources,
                       SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = context;
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.sceneViewportModel.addSceneObjectType("EnvironmentMap");
    }

    @Override
    public void initialize()
    {
        this.rectangleVertices = context.createRectangle();

        try
        {
            this.environmentBackgroundProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();

            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", this.rectangleVertices);
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            log.error("Failed to load shader.", e);
        }
    }

    @Override
    public void reloadShaders()
    {
        ProgramObject<ContextType> newEnvironmentBackgroundProgram = null;
        try
        {
            newEnvironmentBackgroundProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();

            if (this.environmentBackgroundProgram != null)
            {
                this.environmentBackgroundProgram.close();
            }

            this.environmentBackgroundProgram = newEnvironmentBackgroundProgram;
            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", rectangleVertices);
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            log.error("Failed to load shader.", e);
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (lightingResources.getEnvironmentMap() != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.ENVIRONMENT_MAP)
        {
            Matrix4 envMapMatrix = sceneModel.getLightingModel().getEnvironmentMapMatrix();

            environmentBackgroundProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("EnvironmentMap"));
            environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
            environmentBackgroundProgram.setTexture("env", lightingResources.getEnvironmentMap());
            environmentBackgroundProgram.setUniform("model_view", cameraViewport.getView());
            environmentBackgroundProgram.setUniform("projection", cameraViewport.getViewportProjection());
            environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix);
            environmentBackgroundProgram.setUniform("envMapIntensity", sceneModel.getClearColor());

            environmentBackgroundProgram.setUniform("gamma",
                    lightingResources.getEnvironmentMap().isInternalFormatCompressed() ||
                            lightingResources.getEnvironmentMap().getInternalUncompressedColorFormat().dataType != ColorFormat.DataType.FLOATING_POINT
                            ? 1.0f : 2.2f);

            context.getState().disableDepthTest();
            environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            context.getState().enableDepthTest();
        }
    }

    @Override
    public void close() throws Exception
    {
        if (this.environmentBackgroundProgram != null)
        {
            this.environmentBackgroundProgram.close();
            this.environmentBackgroundProgram = null;
        }

        if (environmentBackgroundDrawable != null)
        {
            environmentBackgroundDrawable.close();
            environmentBackgroundDrawable = null;
        }

        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }
    }
}
