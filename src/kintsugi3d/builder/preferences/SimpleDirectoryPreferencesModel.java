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
import kintsugi3d.builder.preferences.serialization.JacksonUserPreferencesSerializer;

import java.nio.file.Path;

public class SimpleDirectoryPreferencesModel implements DirectoryPreferencesModel
{
    private Path previewImagesDirectory = null;
    private Path logFileDirectory = null;
    private Path cacheDirectory = null;

    private SimpleDirectoryPreferencesModel() {}

    public static SimpleDirectoryPreferencesModel createDefault()
    {
        return new SimpleDirectoryPreferencesModel();
    }

    @Override
    public Path getPreviewImagesDirectory()
    {
        return previewImagesDirectory;
    }

    @Override
    public void setCacheDirectory(Path cacheDirectory)
    {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public Path getLogFileDirectory()
    {
        return logFileDirectory;
    }

    @Override
    public void setLogFileDirectory(Path logFileDirectory)
    {
        this.logFileDirectory = logFileDirectory;
    }

    @Override
    public Path getCacheDirectory()
    {
        return cacheDirectory;
    }

    @Override
    public void setPreviewImagesDirectory(Path previewImagesDirectory)
    {
        this.previewImagesDirectory = previewImagesDirectory;
    }

    @Override
    @JsonIgnore
    public Path getPreferencesFileLocation()
    {
        return JacksonUserPreferencesSerializer.getPreferencesFile().toPath();
    }
}
