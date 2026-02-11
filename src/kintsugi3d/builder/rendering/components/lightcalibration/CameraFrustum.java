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

package kintsugi3d.builder.rendering.components.lightcalibration;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.resources.project.GraphicsResources;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CameraFrustum<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final GraphicsResources<ContextType> resources;

    private ViewSelection viewSelection;

    public CameraFrustum(GraphicsResources<ContextType> resources, SceneViewportModel sceneViewportModel)
    {
        super(resources.getContext());
        this.resources = resources;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        float[] frustum =
        {
            0, 0, 0, 1, 1, -1,
            0, 0, 0, -1, -1, -1,
            0, 0, 0, 1, -1, -1,
            0, 0, 0, -1, 1, -1,
            1, 1, -1, 1, -1, -1,
            1, 1, -1, -1, 1, -1,
            -1, -1, -1, 1, -1, -1,
            -1, -1, -1, -1, 1, -1
        };

        return Map.of("position",
            context.createVertexBuffer()
                .setData(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(3, 16, frustum)));
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (resources instanceof GraphicsResourcesImageSpace)
        {
            GraphicsResourcesImageSpace<ContextType> resourcesImgSpace = (GraphicsResourcesImageSpace<ContextType>)resources;

            FramebufferSize size = framebuffer.getSize();

            // Scale to match actual camera frustum.
            Matrix4 snapViewInverse = viewSelection.getSelectedView().quickInverse(0.01f);
            Vector3 frustumDims = viewSelection.getFrustumDimensions();

            this.getDrawable().program().setUniform("color", new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
            this.getProgram().setUniform("model_view",
                cameraViewport.getView().times(snapViewInverse).times(Matrix4.scale(frustumDims)));
            this.getProgram().setUniform("projection", cameraViewport.getViewportProjection());
            this.getDrawable().draw(PrimitiveMode.LINES, cameraViewport.ofFramebuffer(framebuffer));
        }
    }

    public ViewSelection getViewSelection()
    {
        return viewSelection;
    }

    public void setViewSelection(ViewSelection viewSelection)
    {
        this.viewSelection = viewSelection;
    }
}
