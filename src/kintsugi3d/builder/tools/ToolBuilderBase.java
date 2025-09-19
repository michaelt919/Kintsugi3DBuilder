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

package kintsugi3d.builder.tools;

import kintsugi3d.builder.state.*;

abstract class ToolBuilderBase<ToolType> implements ToolBuilder<ToolType>
{
    private ManipulableViewpointModel cameraModel;
    private ManipulableLightingEnvironmentModel lightingEnvironmentModel;
    private ManipulableObjectPoseModel objectModel;
    private SceneViewportModel sceneViewportModel;
    private GeneralSettingsModel settingsModel;

    protected ToolBuilderBase()
    {
    }

    @Override
    public ToolBuilder<ToolType> setCameraModel(ManipulableViewpointModel cameraModel)
    {
        this.cameraModel = cameraModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setLightingEnvironmentModel(ManipulableLightingEnvironmentModel lightingModel)
    {
        this.lightingEnvironmentModel = lightingModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setObjectModel(ManipulableObjectPoseModel objectModel)
    {
        this.objectModel = objectModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel)
    {
        this.sceneViewportModel = sceneViewportModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setSettingsModel(GeneralSettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
        return this;
    }

    ManipulableViewpointModel getCameraModel()
    {
        return cameraModel;
    }

    ManipulableLightingEnvironmentModel getLightingEnvironmentModel()
    {
        return lightingEnvironmentModel;
    }

    ManipulableObjectPoseModel getObjectModel()
    {
        return objectModel;
    }

    SceneViewportModel getSceneViewportModel()
    {
        return sceneViewportModel;
    }

    GeneralSettingsModel getSettingsModel()
    {
        return settingsModel;
    }
}
