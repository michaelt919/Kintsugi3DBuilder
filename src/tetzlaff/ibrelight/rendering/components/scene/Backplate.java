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

package tetzlaff.ibrelight.rendering.components.scene;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.resources.LightingResources;
import tetzlaff.models.BackgroundMode;

import java.io.File;

public class Backplate<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;

    private Program<ContextType> tintedTexProgram;
    private Drawable<ContextType> tintedTexDrawable;
    private VertexBuffer<ContextType> rectangleVertices;

    public Backplate(ContextType context, LightingResources<ContextType> lightingResources, SceneModel sceneModel)
    {
        this.context = context;
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
    }

    @Override
    public void initialize() throws Exception
    {
        this.rectangleVertices = context.createRectangle();

        this.tintedTexProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture_tint.frag"))
                .createProgram();

        this.tintedTexDrawable = context.createDrawable(tintedTexProgram);
        this.tintedTexDrawable.addVertexBuffer("position", this.rectangleVertices);
    }

    @Override
    public void reloadShaders() throws Exception
    {

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
