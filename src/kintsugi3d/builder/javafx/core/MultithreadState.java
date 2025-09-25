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

package kintsugi3d.builder.javafx.core;

import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.LoadOptionsModel;
import kintsugi3d.builder.javafx.multithread.*;
import kintsugi3d.builder.state.*;
import kintsugi3d.builder.state.cards.TabsModel;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.scene.ManipulableLightingEnvironmentModel;
import kintsugi3d.builder.state.scene.ManipulableObjectPoseModel;
import kintsugi3d.builder.state.scene.ManipulableViewpointModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;

public final class MultithreadState implements Kintsugi3DBuilderState
{
    private final ManipulableViewpointModel cameraModel;
    private final ManipulableLightingEnvironmentModel lightingModel;
    private final ManipulableObjectPoseModel objectModel;
    private final CameraViewListModel cameraViewListModel;
    private final ProjectModel projectModel;

    private final GeneralSettingsModel settingsModel;
    private final LoadOptionsModel loadOptionsModel;
    private final SceneViewportModel sceneViewportModel;
    private final CanvasModel canvasModel;
    private final IOModel ioModel;

    private final TabsModel tabsModel;

    private static final MultithreadState INSTANCE = new MultithreadState();

    public static MultithreadState getInstance()
    {
        return INSTANCE;
    }

    private MultithreadState()
    {
        cameraModel = new SynchronizedCameraModel(JavaFXState.getInstance().getCameraModel());
        objectModel = new SynchronizedObjectPoseModel(JavaFXState.getInstance().getObjectModel());
        lightingModel = new SynchronizedLightingEnvironmentModel(JavaFXState.getInstance().getLightingModel());
        cameraViewListModel = new SynchronizedCameraViewListModel(JavaFXState.getInstance().getCameraViewListModel());
        projectModel = new SynchronizedProjectModel(JavaFXState.getInstance().getProjectModel());
        settingsModel = new SynchronizedGeneralSettingsModel(JavaFXState.getInstance().getSettingsModel());
        tabsModel = new SynchronizedTabsModel(JavaFXState.getInstance().getTabModels());
        sceneViewportModel = new SceneViewportModelImpl();
        loadOptionsModel = JavaFXState.getInstance().getLoadOptionsModel();
        canvasModel = new CanvasModelImpl();
        ioModel = new IOModel();
        ioModel.setImageLoadOptionsModel(loadOptionsModel);
    }

    @Override
    public ManipulableViewpointModel getCameraModel()
    {
        return cameraModel;
    }

    @Override
    public ManipulableLightingEnvironmentModel getLightingModel()
    {
        return lightingModel;
    }

    @Override
    public ManipulableObjectPoseModel getObjectModel()
    {
        return objectModel;
    }

    @Override
    public CameraViewListModel getCameraViewListModel()
    {
        return cameraViewListModel;
    }

    @Override
    public TabsModel getTabModels() {
        return tabsModel;
    }

    @Override
    public LoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    @Override
    public GeneralSettingsModel getSettingsModel()
    {
        return settingsModel;
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
