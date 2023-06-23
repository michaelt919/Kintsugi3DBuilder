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

package tetzlaff.ibrelight.javafx;//Created by alexk on 7/19/2017.

import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.core.LoadingModel;
import tetzlaff.ibrelight.core.ReadonlyLoadOptionsModel;
import tetzlaff.ibrelight.javafx.multithread.*;
import tetzlaff.models.*;
import tetzlaff.models.impl.SceneViewportModelImpl;

public final class MultithreadModels implements IBRelightModels
{
    private final ExtendedCameraModel cameraModel;
    private final EnvironmentModel environmentModel;
    private final ExtendedLightingModel lightingModel;
    private final ExtendedObjectModel objectModel;
    private final ProjectModel projectModel;

    private final SettingsModel settingsModel;
    private final ReadonlyLoadOptionsModel loadOptionsModel;
    private final SceneViewportModel sceneViewportModel;
    private final LoadingModel loadingModel;

    private static final MultithreadModels INSTANCE = new MultithreadModels();

    public static MultithreadModels getInstance()
    {
        return INSTANCE;
    }

    private MultithreadModels()
    {
        cameraModel = new CameraModelWrapper(InternalModels.getInstance().getCameraModel());
        objectModel = new ObjectModelWrapper(InternalModels.getInstance().getObjectModel());
        lightingModel = new LightingModelWrapper(InternalModels.getInstance().getLightingModel());
        environmentModel = new EnvironmentModelWrapper(InternalModels.getInstance().getEnvironmentModel());
        projectModel = InternalModels.getInstance().getProjectModel();
        settingsModel = new SettingsModelWrapper(InternalModels.getInstance().getSettingsModel());
        sceneViewportModel = new SceneViewportModelImpl();
        loadOptionsModel = InternalModels.getInstance().getLoadOptionsModel();
        loadingModel = new LoadingModel();
        loadingModel.setLoadOptionsModel(loadOptionsModel);
    }

    @Override
    public ExtendedCameraModel getCameraModel()
    {
        return cameraModel;
    }

    @Override
    public ExtendedLightingModel getLightingModel()
    {
        return lightingModel;
    }

    @Override
    public ExtendedObjectModel getObjectModel()
    {
        return objectModel;
    }

    @Override
    public ReadonlyLoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    @Override
    public SettingsModel getSettingsModel()
    {
        return settingsModel;
    }

    @Override
    public EnvironmentModel getEnvironmentModel()
    {
        return environmentModel;
    }

    @Override
    public LoadingModel getLoadingModel()
    {
        return loadingModel;
    }

    @Override
    public SceneViewportModel getSceneViewportModel()
    {
        return sceneViewportModel;
    }

    @Override
    public ProjectModel getProjectModel()
    {
        return projectModel;
    }
}
