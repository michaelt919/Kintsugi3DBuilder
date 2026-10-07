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

package kintsugi3d.builder.preferences;

import kintsugi3d.builder.io.LoadOptionsModel;
import kintsugi3d.builder.preferences.serialization.JacksonUserPreferencesSerializer;
import kintsugi3d.builder.preferences.serialization.UserPreferencesSerializer;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public final class GlobalUserPreferencesManager
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
        {
            rollback();
        }

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

    /**
     *
     * @param loadOptionsModel The model into which to load options related to content loading.
     * @param settingsModel THe model into which to load general settings.
     */
    public void load(LoadOptionsModel loadOptionsModel, GeneralSettingsModel settingsModel)
    {
        rollback();
        inject(loadOptionsModel, settingsModel);
    }

    /**
     *
     * @param loadOptionsModel The model containing content loading options to save.
     * @param settingsModel THe model containing general settings to save.
     */
    public void save(LoadOptionsModel loadOptionsModel, GeneralSettingsModel settingsModel) throws IOException
    {
        collect(loadOptionsModel, settingsModel);
        commit();
    }

    private void inject(LoadOptionsModel loadOptionsModel, GeneralSettingsModel settingsModel)
    {
        loadOptionsModel.copyFrom(preferencesModel.getLoadOptions());
        settingsModel.copyFrom(preferencesModel.getSettings());
    }

    private void collect(LoadOptionsModel loadOptionsModel, GeneralSettingsModel settingsModel)
    {
        preferencesModel.setLoadOptions(loadOptionsModel);
        preferencesModel.setSettings(settingsModel);
    }

    private void commit() throws IOException
    {
        serializer.writeUserPreferences(preferencesModel);
    }

    private void rollback()
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
