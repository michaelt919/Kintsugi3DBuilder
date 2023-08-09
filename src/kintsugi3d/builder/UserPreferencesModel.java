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

package kintsugi3d.builder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;
import kintsugi3d.builder.core.SimpleLoadOptionsModel;

public class UserPreferencesModel implements ReadOnlyUserPreferencesModel
{
    private SimpleLoadOptionsModel loadOptionsModel = new SimpleLoadOptionsModel();

    private DirectoryPreferencesModel directoryPreferencesModel = DirectoryPreferencesModel.createDefault();

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

    public DirectoryPreferencesModel getDirectoryPreferences()
    {
        return directoryPreferencesModel;
    }

    public void setDirectoryPreferencesModel(DirectoryPreferencesModel directoryPreferencesModel)
    {
        this.directoryPreferencesModel = directoryPreferencesModel;
    }

    public SimpleLoadOptionsModel getLoadOptions()
    {
        return loadOptionsModel;
    }

    public void setLoadOptions(SimpleLoadOptionsModel loadOptionsModel)
    {
        this.loadOptionsModel = loadOptionsModel;
    }
}
