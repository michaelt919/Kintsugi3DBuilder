/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx;

import tetzlaff.ibrelight.javafx.internal.*;

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
    private final LoadOptionsModelImpl loadOptionsModel;
    private final SettingsModelImpl settingsModel;

    private InternalModels()
    {
        cameraModel = new CameraModelImpl();
        environmentModel = new EnvironmentModelImpl();
        objectModel = new ObjectModelImpl();
        lightingModel = new LightingModelImpl(environmentModel);
        loadOptionsModel = new LoadOptionsModelImpl();
        settingsModel = new SettingsModelImpl();
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
}
