/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.app;

import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.io.IOModel;
import kintsugi3d.builder.io.LoadOptionsModel;
import kintsugi3d.builder.javafx.core.JavaFXState;
import kintsugi3d.builder.javafx.multithread.*;
import kintsugi3d.builder.state.*;
import kintsugi3d.builder.state.cards.TabsModel;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.scene.ManipulableLightingEnvironmentModel;
import kintsugi3d.builder.state.scene.ManipulableObjectPoseModel;
import kintsugi3d.builder.state.scene.ManipulableViewpointModel;
import kintsugi3d.builder.state.scene.UserShaderModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;

public final class MultithreadState implements Kintsugi3DBuilderState
{
    private final ManipulableViewpointModel cameraModel;
    private final ManipulableLightingEnvironmentModel lightingModel;
    private final ManipulableObjectPoseModel objectModel;
    private final UserShaderModel userShaderModel;
    private final CameraViewListModel cameraViewListModel;
    private final ProjectModel projectModel;
    private final CarouselModel carouselModel;

    private final GeneralSettingsModel settingsModel;
    private final LoadOptionsModel loadOptionsModel;
    private final CanvasListModel canvasListModel;
    private final IOModel ioModel;
    private final CacheModel cacheModel;

    private final TabsModel tabsModel;

    private static final MultithreadState INSTANCE = new MultithreadState(JavaFXApplication.getState());

    // TODO make private
    public static MultithreadState getInstance()
    {
        return INSTANCE;
    }

    private MultithreadState(JavaFXState base)
    {
        cameraModel = new SynchronizedCameraModel(base.getCameraModel());
        objectModel = new SynchronizedObjectPoseModel(base.getObjectModel());
        lightingModel = new SynchronizedLightingEnvironmentModel(base.getLightingModel());
        userShaderModel = new SynchronizedUserShaderModel(base.getUserShaderModel());
        cameraViewListModel = new SynchronizedCameraViewListModel(base.getCameraViewListModel());
        projectModel = new SynchronizedProjectModel(base.getProjectModel());
        settingsModel = new SynchronizedGeneralSettingsModel(base.getSettingsModel());
        tabsModel = new SynchronizedTabsModel(base.getTabModels());
        carouselModel = new SynchronizedCarouselModel(base.getCarouselModel());

        // All methods are either read-only or inherently asynchronous, so no multithread wrapping needed.
        // (This might not be 100% true with Java's memory model but it wouldn't be improved with the
        // "SynchronizedValue" framework which only ensures that writes happen on the JavaFX thread.
        // If more than that is needed, than we probably need to rework the synchronization for all models.
        // In practice, this hasn't proven to be necessary.)
        cacheModel = base.getCacheModel();


        canvasListModel = new CanvasListModelImpl();

        loadOptionsModel = new SynchronizedLoadOptionsModel(base.getLoadOptionsModel());
        ioModel = new IOModel();
        ioModel.setLoadOptionsModel(loadOptionsModel);
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
    public UserShaderModel getUserShaderModel()
    {
        return  userShaderModel;
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
    public CanvasListModel getCanvasListModel()
    {
        return canvasListModel;
    }

    @Override
    public ProjectModel getProjectModel()
    {
        return projectModel;
    }

    @Override
    public CarouselModel getCarouselModel()
    {
        return carouselModel;
    }

    @Override
    public CacheModel getCacheModel()
    {
        return cacheModel;
    }

    public LoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }
}
