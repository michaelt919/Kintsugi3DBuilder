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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.ViewSet;

@FunctionalInterface
public interface ViewSetWriter
{
    /**
     * Saves a view set to an output stream of bytes
     * @param viewSet The view set
     * @param outputStream The output stream
     * @throws Exception If any errors occur while saving the file.
     */
    void writeToStream(ReadonlyViewSet viewSet, OutputStream outputStream);

    /**
     * Saves a view set to an output file.
     * The view set's root directory will be set to the parent directory of the specified file.
     * @param viewSet The view set
     * @param file The file to save
     * @throws Exception If any errors occur while saving the file.
     */
    default void writeToFile(ViewSet viewSet, File file) throws IOException
    {
        try (OutputStream stream = new FileOutputStream(file))
        {
            if (Objects.equals(viewSet.getRootDirectory(), file.getParentFile()))
            {
                // Same root directory; use the view set unmodified.
                writeToStream(viewSet, stream);
            }
            else
            {
                viewSet.setRootDirectory(file.getParentFile());
                writeToStream(viewSet, stream);
            }
        }
    }
}
