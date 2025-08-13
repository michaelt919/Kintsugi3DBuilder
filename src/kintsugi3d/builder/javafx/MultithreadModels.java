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

import javafx.collections.FXCollections;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.LoadOptionsModel;
import kintsugi3d.builder.javafx.multithread.*;
import kintsugi3d.builder.state.*;
import kintsugi3d.builder.state.impl.CanvasModelImpl;
import kintsugi3d.builder.state.impl.SceneViewportModelImpl;

import java.util.Collections;

public final class MultithreadModels implements Kintsugi3DBuilderState
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

    private final TabModels tabModels;

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
        cameraViewListModel = new CameraViewListModelWrapper(InternalModels.getInstance().getCameraViewListModel());
        projectModel = InternalModels.getInstance().getProjectModel();
        settingsModel = new SettingsModelWrapper(InternalModels.getInstance().getSettingsModel());
        sceneViewportModel = new SceneViewportModelImpl();
        loadOptionsModel = InternalModels.getInstance().getLoadOptionsModel();
        canvasModel = new CanvasModelImpl();
        ioModel = new IOModel();
        ioModel.setImageLoadOptionsModel(loadOptionsModel);
        tabModels = InternalModels.getInstance().getTabModels();
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
    public TabModels getTabModels() {
        return tabModels;
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
