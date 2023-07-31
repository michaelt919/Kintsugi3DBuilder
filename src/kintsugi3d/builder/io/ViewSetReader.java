/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.ViewSet;

import java.io.*;

/**
 * Abstracts the idea of loading a view set from a file as a factory object.
 * Typically, an implementation would support just one file format, but that format is not specified by this interface.
 */
@FunctionalInterface
public interface ViewSetReader
{
    /**
     * Loads a view set from an input stream of bytes
     * @param stream The input stream
     * @return The view set
     */
    ViewSet readFromStream(InputStream stream) throws Exception;

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory will be set to the parent directory of the specified file.
     * @param file The file to load
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    default ViewSet readFromFile(File file) throws Exception
    {
        try (InputStream stream = new FileInputStream(file))
        {
            ViewSet result = readFromStream(stream);
            result.setRootDirectory(file.getParentFile());
            return result;
        }
    }
}
