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
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector4;

public class Grid<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final SceneModel sceneModel;

    public Grid(ContextType context, SceneModel sceneModel)
    {
        super(context);
        this.sceneModel = sceneModel;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws FileNotFoundException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        float[] grid = new float[252];
        for (int i = 0; i < 21; i++)
        {
            grid[i * 12] = i * 0.1f - 1.0f;
            grid[i * 12 + 1] = 0;
            grid[i * 12 + 2] = 1;

            grid[i * 12 + 3] = i * 0.1f - 1.0f;
            grid[i * 12 + 4] = 0;
            grid[i * 12 + 5] = -1;

            grid[i * 12 + 6] = 1;
            grid[i * 12 + 7] = 0;
            grid[i * 12 + 8] = i * 0.1f - 1.0f;

            grid[i * 12 + 9] = -1;
            grid[i * 12 + 10] = 0;
            grid[i * 12 + 11] = i * 0.1f - 1.0f;
        }

        return Map.of("position",
            context.createVertexBuffer()
                .setData(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(3, 84, grid)));
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        // Draw grid
        if (sceneModel.getSettingsModel().getBoolean("is3DGridEnabled"))
        {
            this.getDrawable().program().setUniform("projection", cameraViewport.getViewportProjection());
            this.getDrawable().program().setUniform("model_view", cameraViewport.getView().times(Matrix4.scale(sceneModel.getScale())));
            this.getDrawable().program().setUniform("color", new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
            this.getDrawable().program().setUniform("objectID", 0);
            this.getDrawable().draw(PrimitiveMode.LINES, cameraViewport.ofFramebuffer(framebuffer));
        }
    }
}
