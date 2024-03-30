/*
 *  Copyright (c) Michael Tetzlaff 2024
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering.components.lightcalibration;

import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.BaseScene;
import kintsugi3d.builder.rendering.components.snap.ViewSnappable;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.Context;

public class LightCalibration3DScene<ContextType extends Context<ContextType>> extends BaseScene<ContextType>
{
    private final ViewSnappable viewSnappable;

    /**
     *
     * @param resources
     * @param sceneModel
     * @param sceneViewportModel
     * @param viewSnappable Will be used to determine where the light visual is shown (corresponding to the current snapped view)
     */
    public LightCalibration3DScene(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel,
                                   SceneViewportModel sceneViewportModel, ViewSnappable viewSnappable)
    {
        super(resources, sceneModel, sceneViewportModel);
        this.viewSnappable = viewSnappable;
    }

    @Override
    protected void addPostLitComponents(LightingResources<ContextType> lightingResources)
    {
        // the on-screen representation of lights
        LightCalibrationVisual<ContextType> lightVisual = new LightCalibrationVisual<>(context, sceneViewportModel, sceneModel);
        lightVisual.setViewSnappable(viewSnappable);
        components.add(lightVisual);
    }

}
