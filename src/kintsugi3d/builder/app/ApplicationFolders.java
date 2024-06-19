/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.app;

import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ApplicationFolders
{
    public static final String APP_FOLDER_NAME = "Kintsugi3DBuilder";
    private static final OperatingSystem OS = getCurrentOS();

    public static OperatingSystem getCurrentOS()
    {
        String os = System.getProperty("os.name").toLowerCase();

        // Windows
        if (os.contains("win"))
        {
            return OperatingSystem.WINDOWS;
        }

        // Mac OS
        if (os.contains("mac"))
        {
            return OperatingSystem.MACOS;
        }

        // Linux and Unix
        if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
        {
            return OperatingSystem.UNIX;
        }

        return OperatingSystem.UNKNOWN;
    }

    /**
     * Get the long-term application data folder for the current os and user.
     * @return application folder
     */
    public static Path getUserAppDirectory()
    {
        if (System.getProperty("Kintsugi3D.dataDir") != null)
        {
            File dir = new File(System.getProperty("Kintsugi3D.dataDir"));
            if (dir.mkdirs() && dir.canRead() && dir.canWrite())
            {
                return dir.toPath();
            }
        }

        if (OS == OperatingSystem.WINDOWS)
        {
            return Paths.get(System.getenv("APPDATA"), APP_FOLDER_NAME);
        }

        if (OS == OperatingSystem.MACOS)
        {
            return Paths.get(System.getProperty("user.home"), "Library/Application Support/", APP_FOLDER_NAME);
        }

        if (OS == OperatingSystem.UNIX)
        {
            return Paths.get(System.getProperty("user.home"), "." + APP_FOLDER_NAME);
        }

        return Paths.get(".");
    }

    /**
     * Get the folder most appropriate for storing small temporary files. May overlap with the long-term
     * application data folder, and could be overwritten between sessions.
     * @return cache application folder
     */
    public static Path getUserCacheDirectory()
    {
        if (System.getProperty("Kintsugi3D.cacheDir") != null)
        {
            File dir = new File(System.getProperty("Kintsugi3D.cacheDir"));
            if (dir.mkdirs() && dir.canRead() && dir.canWrite())
            {
                return dir.toPath();
            }
        }

        if (OS == OperatingSystem.WINDOWS)
        {
            return Paths.get(System.getenv("LOCALAPPDATA"), APP_FOLDER_NAME);
        }

        if (OS == OperatingSystem.MACOS)
        {
            // Use Application support on MacOS rather than Caches since it clears Caches frequently, which can cause problems with specular fit.
            return Paths.get(System.getProperty("user.home"), "Library/Application Support/", APP_FOLDER_NAME);
        }

        return getUserAppDirectory().resolve("cache");
    }

    /**
     * Get an application folder based on the current os that is not user dependent.
     * @return system app folder
     */
    public static Path getSystemAppDirectory()
    {
        if (OS == OperatingSystem.WINDOWS)
        {
            return Paths.get(System.getenv("PROGRAMDATA"), APP_FOLDER_NAME);
        }

        if (OS == OperatingSystem.MACOS)
        {
            return Paths.get("/Library/" + APP_FOLDER_NAME);
        }

        return getUserAppDirectory();
    }

    /**
     * Get the folder that the application is currently installed to. Will return null
     * if the app is not installed as a system app.
     * @return installation directory
     */
    public static Path getInstallationDirectory()
    {
        //TODO
        return null;
    }

    public static Path getPreviewImagesRootDirectory()
    {
        Path preferred = GlobalUserPreferencesManager.getInstance().getPreferences().getDirectoryPreferences().getPreviewImagesDirectory();
        if (preferred != null)
            return preferred;

        return getUserCacheDirectory().resolve("preview");
    }

    public static Path getFitCacheRootDirectory()
    {
        Path preferred = GlobalUserPreferencesManager.getInstance().getPreferences().getDirectoryPreferences().getCacheDirectory();
        if (preferred != null)
            return preferred;

        return getUserCacheDirectory().resolve("fit");
    }

    public static Path getLogFileDirectory()
    {
        Path preferred = GlobalUserPreferencesManager.getInstance().getPreferences().getDirectoryPreferences().getLogFileDirectory();
        if (preferred != null)
            return preferred;

        return getUserAppDirectory().resolve("logs");
    }
}
