/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
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
import java.io.FileNotFoundException;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.rendering.components.snap.ViewSnappable;
import kintsugi3d.builder.resources.ibr.IBRResources;
import kintsugi3d.gl.core.*;

public class CameraVisual<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final IBRResources<ContextType> resources;
    private final SceneModel sceneModel;

    private ViewSnappable viewSnappable;

    public CameraVisual(IBRResources<ContextType> resources, SceneViewportModel sceneViewportModel, SceneModel sceneModel)
    {
        super(resources.getContext(), sceneViewportModel, "CameraVisual");
        this.resources = resources;
        this.sceneModel = sceneModel;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws FileNotFoundException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "scene"), "texture_multi_as_single.frag"))
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

    }
}
