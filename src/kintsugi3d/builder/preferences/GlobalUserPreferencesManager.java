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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GlobalUserPreferencesManager
{
    private static final Logger log = LoggerFactory.getLogger(GlobalUserPreferencesManager.class);
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

    public void commit()
    {
        try
        {
            serializer.writeUserPreferences(preferencesModel);
        } catch (IOException e)
        {
            log.error("Failed to write user preferences file", e);
        }
    }

    public void rollback()
    {
        preferencesModel = serializer.readOrDefault();
        modelLoaded = true;
    }
}
