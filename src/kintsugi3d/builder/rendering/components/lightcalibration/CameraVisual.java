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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.resources.ibr.IBRResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public class CameraVisual<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final IBRResources<ContextType> resources;

    private ViewSelection viewSelection;

    public CameraVisual(IBRResources<ContextType> resources, SceneViewportModel sceneViewportModel)
    {
        super(resources.getContext(), sceneViewportModel, "CameraVisual");
        this.resources = resources;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture_imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "colorappearance"), "texture_multi_as_single.frag"))
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
        if (resources instanceof IBRResourcesImageSpace)
        {
            IBRResourcesImageSpace<ContextType> resourcesImgSpace = (IBRResourcesImageSpace<ContextType>)resources;

            FramebufferSize size = framebuffer.getSize();

            this.getContext().getState().disableBackFaceCulling();

            this.getContext().getState().disableDepthWrite();
            this.getContext().getState().enableDepthTest();

            Matrix4 snapViewInverse = viewSelection.getSelectedView().quickInverse(0.01f);
            float aspect = viewSelection.getSelectedCameraProjection().getAspectRatio();
            float fPlane = getCameraPlane(viewSelection);

            this.getProgram().setTexture("viewImages", resourcesImgSpace.colorTextures);
            this.getProgram().setUniform("viewIndex", viewSelection.getSelectedViewIndex());
            this.getProgram().setUniform("model_view",
                cameraViewport.getView().times(snapViewInverse)
                    .times(Matrix4.translate(new Vector3(0, 0, -fPlane)))
                    .times(Matrix4.scale(Math.min(1.0f, aspect), Math.min(1.0f, 1.0f / aspect), 1.0f)));
            this.getProgram().setUniform("projection", cameraViewport.getViewportProjection());
            this.getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));

            this.getContext().getState().enableDepthWrite();
            this.getContext().getState().enableDepthTest();
        }
    }

    public static float getCameraPlane(ViewSelection viewSelection)
    {
        float fy = 1.0f / (float)Math.tan(viewSelection.getSelectedCameraProjection().getVerticalFieldOfView() / 2);
        float fx = fy / viewSelection.getSelectedCameraProjection().getAspectRatio();
        return Math.min(fx, fy);
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
