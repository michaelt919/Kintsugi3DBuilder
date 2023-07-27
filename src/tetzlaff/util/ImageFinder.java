/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Singleton class
 */
public class ImageFinder
{
    private final static Logger log = LoggerFactory.getLogger(ImageFinder.class);

    private static final ImageFinder INSTANCE = new ImageFinder();

    public static ImageFinder getInstance()
    {
        return INSTANCE;
    }

    private ImageFinder()
    {
    }

    // TODO move outside this class
    public File findImageFile(File requestedFile) throws FileNotFoundException
    {
        if (requestedFile.exists())
        {
            return requestedFile;
        }
        else
        {
            // Try some alternate file formats/extensions
            String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG", "tif", "TIF", "tiff", "TIFF" };
            for(String extension : altFormats)
            {
                String[] filenameParts = requestedFile.getName().split("\\.");

                String altFileName;
                if (filenameParts.length > 1)
                {
                    filenameParts[filenameParts.length - 1] = extension;
                    altFileName = String.join(".", filenameParts);
                }
                else
                {
                    altFileName = String.join(".", filenameParts[0], extension);
                }

                File imageFileGuess = new File(requestedFile.getParentFile(), altFileName);

                log.info("Trying '{}'", imageFileGuess.getAbsolutePath());
                if (imageFileGuess.exists())
                {
                    log.info("Found!!");
                    return imageFileGuess;
                }
            }

            // Is it still not there?
            throw new FileNotFoundException(String.format("'%s' not found.", requestedFile.getName()));
        }
    }

    public File tryFindImageFile(File requestedFile)
    {
        try
        {
            return findImageFile(requestedFile);
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
    }
}
