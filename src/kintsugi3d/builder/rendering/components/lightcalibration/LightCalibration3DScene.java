/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering.components.lightcalibration;

import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.BaseScene;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.Context;

public class LightCalibration3DScene<ContextType extends Context<ContextType>> extends BaseScene<ContextType>
{
    private final ViewSelection viewSelection;

    /**
     *
     * @param resources
     * @param sceneModel
     * @param sceneViewportModel
     * @param viewSelection Will be used to determine where the light visual is shown (corresponding to the current snapped view)
     */
    public LightCalibration3DScene(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel,
                                   SceneViewportModel sceneViewportModel, ViewSelection viewSelection)
    {
        super(resources, sceneModel, sceneViewportModel);
        this.viewSelection = viewSelection;
    }

    @Override
    protected void addPostLitComponents(LightingResources<ContextType> lightingResources)
    {
        // the representation of the camera
        CameraVisual<ContextType> cameraVisual = new CameraVisual<>(resources, sceneViewportModel);
        cameraVisual.setViewSelection(viewSelection);
        components.add(cameraVisual);

        // the visualization of the camera frustum
        CameraFrustum<ContextType> cameraFrustum = new CameraFrustum<>(resources, sceneViewportModel);
        cameraFrustum.setViewSelection(viewSelection);
        components.add(cameraFrustum);

        // the on-screen representation of lights
        LightCalibrationVisual<ContextType> lightVisual = new LightCalibrationVisual<>(context, sceneViewportModel, sceneModel);
        lightVisual.setViewSelection(viewSelection);
        components.add(lightVisual);
    }

}
