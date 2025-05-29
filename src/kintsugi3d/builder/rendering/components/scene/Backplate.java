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

package kintsugi3d.builder.rendering.components.scene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.state.BackgroundMode;
import kintsugi3d.gl.core.*;

public class Backplate<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;

    public Backplate(ContextType context, LightingResources<ContextType> lightingResources, SceneModel sceneModel)
    {
        super(context);
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture_tint.frag"))
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
        if (lightingResources.getBackplateTexture() != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.IMAGE)
        {
            getDrawable().program().setTexture("tex", lightingResources.getBackplateTexture());
            getDrawable().program().setUniform("color", sceneModel.getClearColor());

            getContext().getState().disableDepthTest();
            getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            getContext().getState().enableDepthTest();

            // Clear ID buffer again.
            framebuffer.clearIntegerColorBuffer(1, 0, 0, 0, 0);
        }
    }
}
