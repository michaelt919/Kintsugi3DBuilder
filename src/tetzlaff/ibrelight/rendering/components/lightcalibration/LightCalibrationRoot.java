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

package tetzlaff.ibrelight.rendering.components.lightcalibration;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.components.snap.ViewSnap;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;

public class LightCalibrationRoot<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final IBRResourcesImageSpace<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private ViewSnap<ContextType> viewSnapRoot;

    public LightCalibrationRoot(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel,
                                SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;

        this.viewSnapRoot = new ViewSnap<>(sceneModel, resources.getViewSet());
    }

    @Override
    public void initialize() throws Exception
    {
        viewSnapRoot.takeContentRoot(new LightCalibrationContent<>(resources, sceneModel, sceneViewportModel));
        viewSnapRoot.initialize();
    }

    @Override
    public void update() throws Exception
    {
        viewSnapRoot.update();
    }

    @Override
    public void reloadShaders() throws Exception
    {
        viewSnapRoot.reloadShaders();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        viewSnapRoot.draw(framebuffer, cameraViewport);
    }

    @Override
    public void close() throws Exception
    {
        if (viewSnapRoot != null)
        {
            viewSnapRoot.close();
            viewSnapRoot = null;
        }
    }
}
