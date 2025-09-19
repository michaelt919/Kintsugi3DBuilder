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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.ViewSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface ViewSetReader
{
    /**
     * Loads a view set from an input stream.
     * The root directory will be set as specified.
     * @param stream The file to load
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    ViewSet.Builder readFromStream(InputStream stream, ViewSetDirectories directories) throws Exception;

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory will be set to the parent directory of the specified file.
     * @param cameraFile The file to load
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    default ViewSet.Builder readFromFile(File cameraFile, ViewSetDirectories directories) throws Exception
    {
        try (InputStream stream = new FileInputStream(cameraFile))
        {
            directories.projectRoot = cameraFile.getParentFile();
            return readFromStream(stream, directories);
        }
    }

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory will be set to the parent directory of the specified file.
     * @param cameraFile The file to load
     * @param loadOptions Load options to automatically be applied to the view set builder.
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    default ViewSet.Builder readFromFile(File cameraFile, ViewSetLoadOptions loadOptions) throws Exception
    {
        ViewSet.Builder builder = this.readFromFile(cameraFile, loadOptions.mainDirectories);

        if (loadOptions.geometryFile != null)
        {
            builder.setGeometryFile(loadOptions.geometryFile);
        }

        if (loadOptions.masksDirectory != null)
        {
            builder.setMasksDirectory(loadOptions.masksDirectory);
        }

        if (loadOptions.orientationViewName != null)
        {
            builder.setOrientationViewName(loadOptions.orientationViewName)
                .setOrientationViewRotation(loadOptions.orientationViewRotation);
        }

        if (loadOptions.uuid != null)
        {
            builder.setUUID(loadOptions.uuid);
        }

        return builder;
    }
}
