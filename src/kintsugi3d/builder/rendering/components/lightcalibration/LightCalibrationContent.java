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
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.IBRSubject;
import kintsugi3d.builder.rendering.components.snap.ViewSnapContent;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.UniformBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;

public class LightCalibrationContent <ContextType extends Context<ContextType>> extends ViewSnapContent<ContextType>
{
    private final ContextType context;
    private final IBRResourcesImageSpace<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel sceneViewportModel;

    private IBRSubject<ContextType> ibrSubject;

    public LightCalibrationContent(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel,
                                   SceneViewportModel sceneViewportModel)
    {
        this.context = resources.getContext();
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void initialize()
    {
        // the actual subject for image-based rendering
        // No lighting resources since light calibration is effectively unlit shading
        ibrSubject = new IBRSubject<>(resources, sceneViewportModel, sceneModel, null);
        ibrSubject.initialize();
        ibrSubject.setLightCalibrationMode(true);
    }

    @Override
    public void reloadShaders()
    {
        ibrSubject.reloadShaders();
    }

    @Override
    public void update()
    {
        ibrSubject.update();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3();
        Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());

        // Only draw the IBR subject for light calibration, no other components like backplate, grid, ground plane, etc.

        // Hole fill color depends on whether in light calibration mode or not.
        ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.5f));

        Matrix4 lightView = lightTransform.times(cameraViewport.getView());
        Vector3 lightPosWorldSpace = lightView.getUpperLeft3x3().transpose().times(lightView.getColumn(3).getXYZ().negated());
        Vector3 worldSpaceCamAxis = cameraViewport.getView().getRow(2).getXYZ().normalized().negated(); // camera-space -z axis in world space
        Vector3 worldSpaceUpAxis = cameraViewport.getView().getRow(1).getXYZ();

        Vector3 targetPosCamSpace = new Vector3(0, 0, cameraViewport.getView().get(2, 3)); // get z-coordinate of world-space origin in cam-space
        Vector3 targetPosWorldSpace = cameraViewport.getView().getUpperLeft3x3().transpose()
            .times(targetPosCamSpace.minus(cameraViewport.getView().getColumn(3).getXYZ()));

        Matrix4 lookAt = Matrix4.lookAt(lightPosWorldSpace, targetPosWorldSpace, worldSpaceUpAxis);

        try(UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer())
        {
            // TODO byte ordering seems to be OS-dependent when setting up this uniform buffer, or perhaps less tolerant of incomplete GLSL types
            int selectedCameraViewIndex = sceneModel.getCameraViewListModel().getSelectedCameraViewIndex();
            viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance()
                .createFromIntArray(false, 4, 1,
                    selectedCameraViewIndex, selectedCameraViewIndex, selectedCameraViewIndex, selectedCameraViewIndex));
            ibrSubject.getProgram().setUniformBuffer("ViewIndices", viewIndexBuffer);

            // Draw the actual object, without model transformation for light calibration
            ibrSubject.draw(framebuffer, cameraViewport.copyForView(lookAt));
        }

        context.flush();

        // Read buffers after rendering just the IBR subject
        sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
    }

    @Override
    public void close()
    {
        if (ibrSubject != null)
        {
            ibrSubject.close();
            ibrSubject = null;
        }
    }
}
