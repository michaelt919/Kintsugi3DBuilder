/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering.components.lightcalibration;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.UniformBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.IBRSubject;
import kintsugi3d.builder.rendering.components.snap.ViewSnapContent;
import kintsugi3d.builder.resources.IBRResourcesImageSpace;

import java.io.FileNotFoundException;

public class LightCalibrationContent <ContextType extends Context<ContextType>> extends ViewSnapContent<ContextType>
{
    private final ContextType context;
    private final IBRResourcesImageSpace<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private IBRSubject<ContextType> ibrSubject;

    public LightCalibrationContent(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel,
                                   SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.getContext();
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        // the actual subject for image-based rendering
        // No lighting resources since light calibration is effectively unlit shading
        ibrSubject = new IBRSubject<>(resources, null, sceneModel, sceneViewportModel);
        ibrSubject.initialize();
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        ibrSubject.reloadShaders();
    }

    @Override
    public void update() throws FileNotFoundException
    {
        ibrSubject.update();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        drawInSubdivisions(framebuffer, cameraViewport.getWidth(), cameraViewport.getHeight(), cameraViewport);
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight, CameraViewport cameraViewport)
    {
        int primaryLightIndex = this.resources.getViewSet().getLightIndex(this.resources.getViewSet().getPrimaryViewIndex());

        Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3()
                .plus(resources.getViewSet().getLightPosition(primaryLightIndex));
        Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());

        // Only draw the IBR subject for light calibration, no other components like backplate, grid, ground plane, etc.

        // Hole fill color depends on whether in light calibration mode or not.
        ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.5f));

        try(UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer())
        {
            viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance()
                .createFromIntArray(false, 1, 1, getSnapViewIndex()));
            ibrSubject.getProgram().setUniformBuffer("ViewIndices", viewIndexBuffer);

            // Draw the actual object, without model transformation for light calibration
            ibrSubject.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight,
                lightTransform.times(cameraViewport.getView()), cameraViewport.getViewportProjection());
        }

        context.flush();

        // Read buffers after rendering just the IBR subject
        sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
    }

    @Override
    public void close() throws Exception
    {
        if (ibrSubject != null)
        {
            ibrSubject.close();
            ibrSubject = null;
        }
    }
}
