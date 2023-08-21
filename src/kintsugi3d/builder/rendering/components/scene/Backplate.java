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

import kintsugi3d.builder.rendering.IBREngine;
import kintsugi3d.gl.core.*;
import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.state.BackgroundMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class Backplate<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(Backplate.class);
    private final ContextType context;
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;

    private ProgramObject<ContextType> tintedTexProgram;
    private Drawable<ContextType> tintedTexDrawable;
    private VertexBuffer<ContextType> rectangleVertices;

    public Backplate(ContextType context, LightingResources<ContextType> lightingResources, SceneModel sceneModel)
    {
        this.context = context;
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
    }

    @Override
    public void initialize()
    {
        this.rectangleVertices = context.createRectangle();

        try
        {
            this.tintedTexProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture_tint.frag"))
                    .createProgram();
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            log.error("Failed to load shader.", e);
        }

        this.tintedTexDrawable = context.createDrawable(tintedTexProgram);
        this.tintedTexDrawable.addVertexBuffer("position", this.rectangleVertices);
    }

    @Override
    public void reloadShaders()
    {
        if (this.tintedTexProgram != null)
        {

            try
            {
                ProgramObject<ContextType> newProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture_tint.frag"))
                    .createProgram();

                if (this.tintedTexProgram != null)
                {
                    this.tintedTexProgram.close();
                }

                this.tintedTexProgram = newProgram;

                this.tintedTexDrawable = context.createDrawable(tintedTexProgram);
                this.tintedTexDrawable.addVertexBuffer("position", this.rectangleVertices);
            }
            catch (FileNotFoundException|RuntimeException e)
            {
                log.error("Failed to load shader.", e);
            }
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (lightingResources.getBackplateTexture() != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.IMAGE)
        {
            tintedTexDrawable.program().setTexture("tex", lightingResources.getBackplateTexture());
            tintedTexDrawable.program().setUniform("color", sceneModel.getClearColor());

            context.getState().disableDepthTest();
            tintedTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            context.getState().enableDepthTest();

            // Clear ID buffer again.
            framebuffer.clearIntegerColorBuffer(1, 0, 0, 0, 0);
        }
    }

    @Override
    public void close() throws Exception
    {
        if (tintedTexProgram != null)
        {
            tintedTexProgram.close();
            tintedTexProgram = null;
        }

        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }
    }
}
