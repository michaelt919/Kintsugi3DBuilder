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

package kintsugi3d.builder.resources.project;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class MissingImagesException extends Exception
{
    private static final long serialVersionUID = 4726434475341975631L;
    private final Collection<File> missingImgs;
    private final File imgDirectory;

    public MissingImagesException(String message, Collection<File> missingImgs)
    {
        super(message);
        this.missingImgs = List.copyOf(missingImgs);
        imgDirectory = null;
    }

    public MissingImagesException(String message, Collection<File> missingImgs, File imgDirectory)
    {
        super(message);
        this.missingImgs = List.copyOf(missingImgs);
        this.imgDirectory = imgDirectory;
    }

    public Collection<File> getMissingImgs()
    {
        return missingImgs;
    }

    public File getImgDirectory()
    {
        return imgDirectory;
    }
}
