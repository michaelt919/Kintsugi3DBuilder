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

    private final CameraModelImpl cameraModel;
    private final EnvironmentModelImpl environmentModel;
    private final LightingModelImpl lightingModel;
    private final ObjectModelImpl objectModel;
    private final CameraViewListModelImpl cameraViewListModel;
    private final LoadOptionsModelImpl loadOptionsModel;
    private final SettingsModelImpl settingsModel;
    private final ObservableProjectModel projectModel;
    private final TabModelsImpl tabModels;

    private JavaFXState()
    {
        cameraModel = new CameraModelImpl();
        environmentModel = new EnvironmentModelImpl();
        objectModel = new ObjectModelImpl();
        lightingModel = new LightingModelImpl(environmentModel);
        cameraViewListModel = new CameraViewListModelImpl();
        loadOptionsModel = new LoadOptionsModelImpl();
        settingsModel = new SettingsModelImpl();
        projectModel = new ObservableProjectModel();
        tabModels = new TabModelsImpl();
    }

    public CameraModelImpl getCameraModel()
    {
        return cameraModel;
    }

    public LightingModelImpl getLightingModel()
    {
        return lightingModel;
    }

    public ObjectModelImpl getObjectModel()
    {
        return objectModel;
    }

    public CameraViewListModelImpl getCameraViewListModel()
    {
        return cameraViewListModel;
    }

    public CardsModel getCardsModel(String label) { return tabModels.getCardsModel(label); }

    public TabModels getTabModels() { return tabModels; }

    public LoadOptionsModelImpl getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    public SettingsModelImpl getSettingsModel()
    {
        return settingsModel;
    }

    public EnvironmentModelImpl getEnvironmentModel()
    {
        return environmentModel;
    }

    public ObservableProjectModel getProjectModel()
    {
        return projectModel;
    }
}
