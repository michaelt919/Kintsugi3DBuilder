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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.ViewSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface ViewSetReaderFromLooseFiles
{
    /**
     * Loads a view set from an input stream.
     * The root directory will be set as specified.
     * @param stream The file to load
     * @param root
     * @param geometryFile
     * @param fullResImageDirectory
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    ViewSet readFromStream(InputStream stream, File root, File geometryFile, File fullResImageDirectory) throws Exception;

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory will be set to the parent directory of the specified file.
     * @param cameraFile The file to load
     * @param geometryFile
     * @param fullResImageDirectory
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    default ViewSet readFromFile(File cameraFile, File geometryFile, File fullResImageDirectory) throws Exception
    {
        try (InputStream stream = new FileInputStream(cameraFile))
        {
            return readFromStream(stream, cameraFile.getParentFile(), geometryFile, fullResImageDirectory);
        }
    }
}
