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

package kintsugi3d.builder.javafx;//Created by alexk on 7/19/2017.

import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.LoadOptionsModel;
import kintsugi3d.builder.javafx.multithread.*;
import kintsugi3d.builder.state.*;
import kintsugi3d.builder.state.impl.CanvasModelImpl;
import kintsugi3d.builder.state.impl.SceneViewportModelImpl;

public final class MultithreadState implements Kintsugi3DBuilderState
{
    private final ExtendedCameraModel cameraModel;
    private final EnvironmentModel environmentModel;
    private final ExtendedLightingModel lightingModel;
    private final ExtendedObjectModel objectModel;
    private final CameraViewListModel cameraViewListModel;
    private final ProjectModel projectModel;

    private final SettingsModel settingsModel;
    private final LoadOptionsModel loadOptionsModel;
    private final SceneViewportModel sceneViewportModel;
    private final CanvasModel canvasModel;
    private final IOModel ioModel;

    private static final MultithreadState INSTANCE = new MultithreadState();

    public static MultithreadState getInstance()
    {
        return INSTANCE;
    }

    private MultithreadState()
    {
        cameraModel = new CameraModelWrapper(JavaFXState.getInstance().getCameraModel());
        objectModel = new ObjectModelWrapper(JavaFXState.getInstance().getObjectModel());
        lightingModel = new LightingModelWrapper(JavaFXState.getInstance().getLightingModel());
        environmentModel = new EnvironmentModelWrapper(JavaFXState.getInstance().getEnvironmentModel());
        cameraViewListModel = new CameraViewListModelWrapper(JavaFXState.getInstance().getCameraViewListModel());
        projectModel = JavaFXState.getInstance().getProjectModel();
        settingsModel = new SettingsModelWrapper(JavaFXState.getInstance().getSettingsModel());
        sceneViewportModel = new SceneViewportModelImpl();
        loadOptionsModel = JavaFXState.getInstance().getLoadOptionsModel();
        canvasModel = new CanvasModelImpl();
        ioModel = new IOModel();
        ioModel.setImageLoadOptionsModel(loadOptionsModel);
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
    public CameraViewListModel getCameraViewListModel()
    {
        return cameraViewListModel;
    }

    @Override
    public LoadOptionsModel getLoadOptionsModel()
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
    public IOModel getIOModel()
    {
        return ioModel;
    }

    @Override
    public CanvasModel getCanvasModel()
    {
        return canvasModel;
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
