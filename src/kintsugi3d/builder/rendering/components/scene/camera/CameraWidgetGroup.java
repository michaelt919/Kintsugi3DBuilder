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

package kintsugi3d.builder.rendering.components.scene.camera;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.lightcalibration.CameraFrustum;
import kintsugi3d.builder.rendering.components.lightcalibration.CameraVisual;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.rendering.components.snap.ViewSelectionImpl;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;

public class CameraWidgetGroup<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private int cameraIndex;

    private ViewSelection selection;

    private IBRResourcesImageSpace<ContextType> resources;
    private SceneModel sceneModel;
    private SceneViewportModel sceneViewportModel;

    private CameraVisual<ContextType> cameraVisual;
    private CameraFrustum<ContextType> cameraFrustum;

    public CameraWidgetGroup(IBRResourcesImageSpace<ContextType> resources,
                             SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;

        selection = new ViewSelectionImpl(resources.getViewSet(), sceneModel){
            @Override
            public int getSelectedViewIndex()
            {
                return cameraIndex;
            }
        };

        cameraVisual = new CameraVisual<>(resources, sceneViewportModel);
        cameraVisual.setViewSelection(selection);
        cameraFrustum = new CameraFrustum<>(resources, sceneViewportModel);
        cameraFrustum.setViewSelection(selection);
    }

    @Override
    public void initialize()
    {
        cameraVisual.initialize();
        cameraFrustum.initialize();
    }

    @Override
    public void reloadShaders()
    {
        cameraVisual.reloadShaders();
        cameraFrustum.reloadShaders();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (sceneModel.getSettingsModel().getBoolean("isCameraVisualEnabled"))
        {
            for (cameraIndex = 0; cameraIndex < resources.getViewSet().getCameraPoseCount(); cameraIndex++)
            {
                cameraVisual.draw(framebuffer, cameraViewport);
                cameraFrustum.draw(framebuffer, cameraViewport);
            }
        }
    }

    @Override
    public void close()
    {
        cameraVisual.close();
        cameraFrustum.close();
    }
}
