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

package kintsugi3d.builder.preferences;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kintsugi3d.builder.core.LoadOptionsModel;
import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;
import kintsugi3d.builder.core.SimpleLoadOptionsModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.builder.state.settings.SimpleGeneralSettingsModel;

public class UserPreferencesModel implements ReadOnlyUserPreferencesModel
{
    private LoadOptionsModel loadOptionsModel = new SimpleLoadOptionsModel();

    private DirectoryPreferencesModel directoryPreferencesModel = SimpleDirectoryPreferencesModel.createDefault();

    private GeneralSettingsModel settingsModel = new SimpleGeneralSettingsModel();

    private UserPreferencesModel() {}

    public static UserPreferencesModel createDefault()
    {
        return new UserPreferencesModel();
    }

    public static UserPreferencesModel createEmpty()
    {
        UserPreferencesModel model = new UserPreferencesModel();
        model.loadOptionsModel = null;
        model.directoryPreferencesModel = null;

        return model;
    }

    @Override
    @JsonIgnore
    public ReadonlyLoadOptionsModel getReadOnlyLoadOptions()
    {
        return loadOptionsModel;
    }

    @Override
    @JsonIgnore
    public ReadOnlyDirectoryPreferencesModel getReadOnlyDirectoryPreferences()
    {
        return directoryPreferencesModel;
    }

    @Override
    @JsonIgnore
    public ReadonlyGeneralSettingsModel getReadOnlySettings()
    {
        return settingsModel;
    }

    public DirectoryPreferencesModel getDirectoryPreferences()
    {
        return directoryPreferencesModel;
    }

    public void setDirectoryPreferences(DirectoryPreferencesModel directoryPreferencesModel)
    {
        this.directoryPreferencesModel = directoryPreferencesModel;
    }

    public LoadOptionsModel getLoadOptions()
    {
        return loadOptionsModel;
    }

    public void setLoadOptions(LoadOptionsModel loadOptionsModel)
    {
        this.loadOptionsModel = loadOptionsModel;
    }

    public GeneralSettingsModel getSettings()
    {
        return settingsModel;
    }

    public void setSettings(GeneralSettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
    }
}
