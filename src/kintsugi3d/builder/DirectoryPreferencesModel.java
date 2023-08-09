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

import java.nio.file.Path;

public class DirectoryPreferencesModel implements ReadOnlyDirectoryPreferencesModel
{
    private Path previewImagesDirectory = null;
    private Path logFileDirectory = null;
    private Path cacheDirectory = null;

    private DirectoryPreferencesModel() {}

    public static DirectoryPreferencesModel createDefault()
    {
        return new DirectoryPreferencesModel();
    }

    @Override
    public Path getPreviewImagesDirectory()
    {
        return previewImagesDirectory;
    }

    public void setCacheDirectory(Path cacheDirectory)
    {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public Path getLogFileDirectory()
    {
        return logFileDirectory;
    }

    public void setLogFileDirectory(Path logFileDirectory)
    {
        this.logFileDirectory = logFileDirectory;
    }

    @Override
    public Path getCacheDirectory()
    {
        return cacheDirectory;
    }

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
