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

package kintsugi3d.builder.util;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.app.OperatingSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

public class Kintsugi3DViewerLauncher
{
    /**
     * Attempts to find the executable location of an installed instance of Kintsugi 3D Viewer
     * Searches first in the PATH env variable, then the registry (Windows only), then by relative path
     * @return File to the viewer executable, if found
     */
    public static Optional<File> getViewerExecutableLocation()
    {
        Optional<File> executable = getFromPath();

        if (executable.isEmpty())
        {
            try
            {
                executable = getFromRegistry();
            }
            catch (IOException | InterruptedException ignored)
            {
            }
        }

        if (executable.isEmpty())
        {
            executable = getExecFromDirectory(new File("."));
        }

        if (executable.isEmpty())
        {
            // Try to get up to Applications directory on MacOS.
            executable = getExecFromDirectory(new File("../../.."));
        }

        return executable;
    }

    private static Optional<File> getFromPath()
    {
        String path = System.getenv("PATH");

        for (String dirStr : path.split("[;:](?!\\\\)"))
        {
            File dir = new File(dirStr);
            Optional<File> exec = getExecFromDirectory(dir);
            if (exec.isPresent())
                return exec;
        }

        return Optional.empty();
    }

    private static Optional<File> getFromRegistry() throws IOException, InterruptedException
    {
        if (ApplicationFolders.getCurrentOS() != OperatingSystem.WINDOWS)
            return Optional.empty();

        ProcessBuilder builder = new ProcessBuilder("reg", "query",
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\Kintsugi3DViewer");
        Process reg = builder.start();
        Optional<String> key;
        try (BufferedReader output = new BufferedReader(
                new InputStreamReader(reg.getInputStream(), StandardCharsets.UTF_8))) {

            Stream<String> keys = output.lines().filter(l -> !l.isEmpty());
            Stream<String> matches = keys.filter(l -> l.contains("Install_Dir"));
            key = matches.findFirst();
        }
        reg.waitFor();

        if (key.isPresent())
        {
            String[] keyStrArr = key.get().split("    ");
            String keyStr = keyStrArr[keyStrArr.length - 1];
            File directory = new File(keyStr);
            return getExecFromDirectory(directory);
        }

        return Optional.empty();
    }

    private static Optional<File> getExecFromDirectory(File dir)
    {
        // Match "Kintsugi3DViewer", "Kintsugi3DViewer.exe" and "Kintsugi3DViewer.app"
        File[] foundFiles = dir.listFiles((dir1, name) -> name.matches("^Kintsugi3DViewer((.exe)|(.app))?$"));
        if (foundFiles != null && foundFiles.length > 0)
        {
            return Optional.of(foundFiles[0]);
        }

        return Optional.empty();
    }

    /**
     * Launches a new instance of Kintsugi 3D Viewer with no model
     * @throws IOException Unknown Error
     * @throws IllegalStateException Kintsugi 3D Viewer is not installed or not found
     */
    public static void launchViewer() throws IOException, IllegalStateException
    {
        launchViewer(null);
    }

    /**
     * Launches a new instance of Kintsugi 3D Viewer with the given glb or gltf file as a parameter to open
     * @param modelFile Path to the model file
     * @throws IOException Unknown error
     * @throws IllegalStateException Kintsugi 3D Viewer is not installed or not found
     */
    public static void launchViewer(File modelFile) throws IOException, IllegalStateException
    {
        Optional<File> execOpt = getViewerExecutableLocation();
        if (execOpt.isEmpty())
            throw new IllegalStateException("Kintsugi 3D Viewer is not installed, or the executable could not be found");
        File executable = execOpt.get();

        String parameter = "";
        if (modelFile != null)
            parameter = modelFile.getAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath(), parameter);
        pb.start();
    }
}
