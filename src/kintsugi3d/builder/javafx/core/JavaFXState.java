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
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabModels;

public final class JavaFXState
{
    private static final JavaFXState INSTANCE = new JavaFXState();

    static JavaFXState getInstance()
    {
        return INSTANCE;
    }

    private final ObservableCameraModel cameraModel;
    private final ObservableEnvironmentModel environmentModel;
    private final ObservableLightingModel lightingModel;
    private final ObservableObjectModel objectModel;
    private final ObservableCameraViewListModel cameraViewListModel;
    private final ObservableLoadOptionsModel loadOptionsModel;
    private final ObservableSettingsModel settingsModel;
    private final ObservableProjectModel projectModel;
    private final TabModelsImpl tabModels;

    private JavaFXState()
    {
        cameraModel = new ObservableCameraModel();
        environmentModel = new ObservableEnvironmentModel();
        objectModel = new ObservableObjectModel();
        lightingModel = new ObservableLightingModel(environmentModel);
        cameraViewListModel = new ObservableCameraViewListModel();
        loadOptionsModel = new ObservableLoadOptionsModel();
        settingsModel = new ObservableSettingsModel();
        projectModel = new ObservableProjectModel();
        tabModels = new TabModelsImpl();
    }

    public ObservableCameraModel getCameraModel()
    {
        return cameraModel;
    }

    public ObservableLightingModel getLightingModel()
    {
        return lightingModel;
    }

    public ObservableObjectModel getObjectModel()
    {
        return objectModel;
    }

    public ObservableCameraViewListModel getCameraViewListModel()
    {
        return cameraViewListModel;
    }

    public CardsModel getCardsModel(String label) { return tabModels.getCardsModel(label); }

    public TabModels getTabModels() { return tabModels; }

    public ObservableLoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    public ObservableSettingsModel getSettingsModel()
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
