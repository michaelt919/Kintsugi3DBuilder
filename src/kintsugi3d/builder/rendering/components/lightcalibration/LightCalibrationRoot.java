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

package kintsugi3d.builder.rendering.components.lightcalibration;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.rendering.components.snap.ViewSnap;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;

public class LightCalibrationRoot<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final IBRResourcesImageSpace<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel sceneViewportModel;

    private ViewSnap<ContextType> viewSnapRoot;

    public LightCalibrationRoot(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel,
                                ViewSelection viewSelection, SceneViewportModel sceneViewportModel)
    {
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;

        this.viewSnapRoot = new ViewSnap<>(sceneModel, viewSelection);
    }

    @Override
    public void initialize()
    {
        viewSnapRoot.takeContentRoot(new LightCalibrationContent<>(resources, sceneModel, sceneViewportModel));
        viewSnapRoot.initialize();
    }

    @Override
    public void update()
    {
        viewSnapRoot.update();
    }

    @Override
    public void reloadShaders()
    {
        viewSnapRoot.reloadShaders();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        viewSnapRoot.draw(framebuffer, cameraViewport);
    }

    @Override
    public void close()
    {
        if (viewSnapRoot != null)
        {
            viewSnapRoot.close();
            viewSnapRoot = null;
        }
    }
}
