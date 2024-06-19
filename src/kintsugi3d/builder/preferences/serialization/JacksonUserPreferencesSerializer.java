/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.preferences.serialization;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.preferences.ReadOnlyUserPreferencesModel;
import kintsugi3d.builder.preferences.UserPreferencesModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JacksonUserPreferencesSerializer implements UserPreferencesSerializer
{
    private static final File DIRECTORY = ApplicationFolders.getUserAppDirectory().toFile();
    private static final String FILE_NAME = "preferences.json";
    private final List<Exception> startupExceptions = new ArrayList<>();

    @Override
    public void writeUserPreferences(ReadOnlyUserPreferencesModel preferencesModel) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(getPreferencesFile(), preferencesModel);
    }

    @Override
    public UserPreferencesModel readUserPreferences() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ObjectReader reader = mapper.readerFor(UserPreferencesModel.class);
        return reader.readValue(getPreferencesFile());
    }

    @Override
    public UserPreferencesModel readOrDefault()
    {
        try
        {
            return readUserPreferences();
        } catch (IOException e)
        {
            startupExceptions.add(e);
            return UserPreferencesModel.createDefault();
        }
    }

    @Override
    public List<Exception> getStartupExceptions()
    {
        return startupExceptions;
    }

    public static File getPreferencesFile()
    {
        return new File(DIRECTORY, FILE_NAME);
    }
}
