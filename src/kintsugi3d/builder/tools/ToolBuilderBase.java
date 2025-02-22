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
    private ToolBindingModel toolBindingModel;
    private ExtendedCameraModel cameraModel;
    private EnvironmentModel environmentModel;
    private ExtendedLightingModel lightingModel;
    private ExtendedObjectModel objectModel;
    private SceneViewportModel sceneViewportModel;
    private SettingsModel settingsModel;

    protected ToolBuilderBase()
    {
    }

    @Override
    public ToolBuilder<ToolType> setToolBindingModel(ToolBindingModel toolBindingModel)
    {
        this.toolBindingModel = toolBindingModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setEnvironmentMapModel(EnvironmentModel environmentModel)
    {
        this.environmentModel = environmentModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setLightingModel(ExtendedLightingModel lightingModel)
    {
        this.lightingModel = lightingModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setObjectModel(ExtendedObjectModel objectModel)
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
    public ToolBuilder<ToolType> setSettingsModel(SettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
        return this;
    }

    ToolBindingModel getToolBindingModel()
    {
        return toolBindingModel;
    }

    ExtendedCameraModel getCameraModel()
    {
        return cameraModel;
    }

    EnvironmentModel getEnvironmentModel()
    {
        return environmentModel;
    }

    ExtendedLightingModel getLightingModel()
    {
        return lightingModel;
    }

    ExtendedObjectModel getObjectModel()
    {
        return objectModel;
    }

    SceneViewportModel getSceneViewportModel()
    {
        return sceneViewportModel;
    }

    SettingsModel getSettingsModel()
    {
        return settingsModel;
    }
}
