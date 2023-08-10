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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import kintsugi3d.builder.app.ApplicationFolders;

import java.io.File;
import java.io.IOException;

public class JacksonUserPreferencesSerializer implements UserPreferencesSerializer
{
    private static final File DIRECTORY = ApplicationFolders.getUserAppDirectory();
    private static final String FILE_NAME = "preferences.json";

    @Override
    public void writeUserPreferences(ReadOnlyUserPreferencesModel preferencesModel) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(getPreferencesFile(), preferencesModel);
    }

    @Override
    public UserPreferencesModel readUserPreferences() throws IOException
    {
        ObjectReader reader = new ObjectMapper().readerFor(UserPreferencesModel.class);
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
            return UserPreferencesModel.createDefault();
        }
    }

    public static File getPreferencesFile()
    {
        return new File(DIRECTORY, FILE_NAME);
    }
}
