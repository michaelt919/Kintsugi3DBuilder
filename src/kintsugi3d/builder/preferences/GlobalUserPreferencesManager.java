/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.preferences;

import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.preferences.serialization.JacksonUserPreferencesSerializer;
import kintsugi3d.builder.preferences.serialization.UserPreferencesSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class GlobalUserPreferencesManager
{
    private static final GlobalUserPreferencesManager INSTANCE = new GlobalUserPreferencesManager();
    private final UserPreferencesSerializer serializer = new JacksonUserPreferencesSerializer();

    private UserPreferencesModel preferencesModel;
    private boolean modelLoaded = false;

    private GlobalUserPreferencesManager() {}

    public static GlobalUserPreferencesManager getInstance()
    {
        return INSTANCE;
    }

    public UserPreferencesModel getPreferences()
    {
        if (!modelLoaded)
            rollback();

        return preferencesModel;
    }

    public void setPreferences(UserPreferencesModel model)
    {
        this.preferencesModel = model;
    }

    public List<Exception> getSerializerStartupExceptions()
    {
        return serializer.getStartupExceptions();
    }

    public void load()
    {
        rollback();
        inject();
    }

    public void save() throws IOException
    {
        collect();
        commit();
    }

    private void inject()
    {
        MultithreadModels.getInstance().getLoadOptionsModel().copyFrom(preferencesModel.getLoadOptions());
        MultithreadModels.getInstance().getSettingsModel().copyFrom(preferencesModel.getSettings());
    }

    private void collect()
    {
        preferencesModel.setLoadOptions(MultithreadModels.getInstance().getLoadOptionsModel());
        preferencesModel.setSettings(MultithreadModels.getInstance().getSettingsModel());
    }

    public void commit() throws IOException
    {
        serializer.writeUserPreferences(preferencesModel);
    }

    public void rollback()
    {
        preferencesModel = serializer.readOrDefault();
        modelLoaded = true;
    }

    public boolean hasStartupFailures()
    {
        return this.getSerializerStartupExceptions().stream()
                .anyMatch(e -> !(e instanceof FileNotFoundException));
    }
}
