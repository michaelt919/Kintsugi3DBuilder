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

package kintsugi3d.builder.javafx;

import kintsugi3d.builder.javafx.internal.*;

public final class InternalModels
{
    private static final InternalModels INSTANCE = new InternalModels();

    static InternalModels getInstance()
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

    private InternalModels()
    {
        cameraModel = new CameraModelImpl();
        environmentModel = new EnvironmentModelImpl();
        objectModel = new ObjectModelImpl();
        lightingModel = new LightingModelImpl(environmentModel);
        cameraViewListModel = new CameraViewListModelImpl();
        loadOptionsModel = new LoadOptionsModelImpl();
        settingsModel = new SettingsModelImpl();
        projectModel = new ObservableProjectModel();
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
