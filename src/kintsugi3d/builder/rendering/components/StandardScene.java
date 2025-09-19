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

package kintsugi3d.builder.rendering.components;

import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.scene.camera.CameraWidgetGroup;
import kintsugi3d.builder.rendering.components.scene.light.LightVisualsGroup;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.gl.core.Context;

public class StandardScene<ContextType extends Context<ContextType>> extends BaseScene<ContextType>
{
    public StandardScene(GraphicsResourcesImageSpace<ContextType> resources, SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        super(resources, sceneModel, sceneViewportModel);
    }

    @Override
    protected void addPostLitComponents(LightingResources<ContextType> lightingResources)
    {
        // the on-screen representation of lights
        components.add(new LightVisualsGroup<>(context, sceneModel, sceneViewportModel));
        components.add(new CameraWidgetGroup<>(resources, sceneModel, sceneViewportModel));
    }

}
