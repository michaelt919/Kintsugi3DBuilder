/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers;

import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.util.RecentProjects;

import java.io.File;
import java.util.Objects;

public final class ProjectLoadHelper
{
    private ProjectLoadHelper()
    {
    }

    public static void startLoad(File projectFile, File vsetFile)
    {
        MultithreadModels.getInstance().getLoadingModel().unload();

        RecentProjects.updateRecentFiles(projectFile.getAbsolutePath());

        if (Objects.equals(projectFile.getParentFile(), vsetFile.getParentFile()))
        {
            // VSET file is the project file or they're in the same directory.
            // Use a supporting files directory underneath by default
            new Thread(() -> MultithreadModels.getInstance().getLoadingModel()
                .loadFromVSETFile(vsetFile.getPath(), vsetFile, ViewSet.getDefaultSupportingFilesDirectory(projectFile)))
                .start();
        }
        else
        {
            // VSET file is presumably already in a supporting files directory, so just use that directory by default
            new Thread(() -> MultithreadModels.getInstance().getLoadingModel()
                .loadFromVSETFile(vsetFile.getPath(), vsetFile))
                .start();
        }
    }
}
