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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.ViewSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstracts the idea of loading a view set from a file as a factory object.
 * Typically, an implementation would support just one file format, but that format is not specified by this interface.
 */
@FunctionalInterface
public interface ViewSetReader
{

    /**
     * Loads a view set from an input file.
     * The root directory and the supporting files directory will be set as specified.
     * The supporting files directory may be overridden by a directory specified in the file.
     * @param stream The file to load
     * @param root
     * @param supportingFilesDirectory
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    ViewSet readFromStream(InputStream stream, File root, File supportingFilesDirectory) throws Exception;

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory as well as the supporting files directory will be set to the specified root.
     * The supporting files directory may be overridden by a directory specified in the file.
     * @param stream
     * @param root
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    default ViewSet readFromStream(InputStream stream, File root) throws Exception
    {
        // Use root directory as supporting files directory
        return readFromStream(stream, root, root);
    }

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory will be set to the parent directory of the specified file.
     * The supporting files directory will be set as specified by default but may be overridden by a directory specified in the file.
     * @param file The file to load
     * @param supportingFilesDirectory
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    default ViewSet readFromFile(File file, File supportingFilesDirectory) throws Exception
    {
        try (InputStream stream = new FileInputStream(file))
        {
            return readFromStream(stream, file.getParentFile(), supportingFilesDirectory);
        }
    }

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory and supporting files direcotry will be set to the parent directory of the specified file.
     * The supporting files directory may be overridden by a directory specified in the file.
     * @param file The file to load
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    default ViewSet readFromFile(File file) throws Exception
    {
        try (InputStream stream = new FileInputStream(file))
        {
            return readFromStream(stream, file.getParentFile());
        }
    }
}
