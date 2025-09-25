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

import kintsugi3d.builder.javafx.internal.*;

public final class JavaFXState
{
    private static final JavaFXState INSTANCE = new JavaFXState();

    static JavaFXState getInstance()
    {
        return INSTANCE;
    }

    private final ObservableCameraModel cameraModel;
    private final ObservableEnvironmentModel environmentModel;
    private final ObservableLightingEnvironmentModel lightingModel;
    private final ObservableObjectPoseModel objectModel;
    private final ObservableUserShaderModel userShaderModel;
    private final ObservableCameraViewListModel cameraViewListModel;
    private final ObservableLoadOptionsModel loadOptionsModel;
    private final ObservableGeneralSettingsModel settingsModel;
    private final ObservableProjectModel projectModel;
    private final ObservableTabsModel tabModels;

    private JavaFXState()
    {
        cameraModel = new ObservableCameraModel();
        environmentModel = new ObservableEnvironmentModel();
        objectModel = new ObservableObjectPoseModel();
        lightingModel = new ObservableLightingEnvironmentModel(environmentModel);
        userShaderModel = new ObservableUserShaderModel();
        cameraViewListModel = new ObservableCameraViewListModel();
        loadOptionsModel = new ObservableLoadOptionsModel();
        settingsModel = new ObservableGeneralSettingsModel();
        projectModel = new ObservableProjectModel();
        tabModels = new ObservableTabsModel();
    }

    public ObservableCameraModel getCameraModel()
    {
        return cameraModel;
    }

    public ObservableLightingEnvironmentModel getLightingModel()
    {
        return lightingModel;
    }

    public ObservableObjectPoseModel getObjectModel()
    {
        return objectModel;
    }

    public ObservableUserShaderModel getUserShaderModel()
    {
        return userShaderModel;
    }

    public ObservableCameraViewListModel getCameraViewListModel()
    {
        return cameraViewListModel;
    }

    public ObservableTabsModel getTabModels() { return tabModels; }

    public ObservableLoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    public ObservableGeneralSettingsModel getSettingsModel()
    {
        return settingsModel;
    }

    public ObservableEnvironmentModel getEnvironmentModel()
    {
        return environmentModel;
    }

    public ObservableProjectModel getProjectModel()
    {
        return projectModel;
    }
}
