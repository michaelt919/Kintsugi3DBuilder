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

package kintsugi3d.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Singleton class
 */
public class ImageFinder
{
    private static final Logger log = LoggerFactory.getLogger(ImageFinder.class);

    private static final ImageFinder INSTANCE = new ImageFinder();
    private static final String[] altFormats  = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG", "tif", "TIF", "tiff", "TIFF" };

    public static ImageFinder getInstance()
    {
        return INSTANCE;
    }

    private ImageFinder()
    {
    }

    public String[] getSupportedImgFormats(){return altFormats;}

    public File findImageFile(File requestedFile) throws FileNotFoundException
    {
        if (requestedFile.exists())
        {
            return requestedFile;
        }
        else
        {
            // Try some alternate file formats/extensions
            // Try appending first (will catch filenames that contain .'s but omit the extension)
            for(String extension : altFormats)
            {
                String altFileName = String.join(".", requestedFile.getName(), extension);
                File imageFileGuess = new File(requestedFile.getParentFile(), altFileName);

                log.info("Trying '{}'", imageFileGuess.getAbsolutePath());
                if (imageFileGuess.exists())
                {
                    log.info("Found!!");
                    return imageFileGuess;
                }
            }

            // try substituting the part after the last . with various extensions
            for(String extension : altFormats)
            {
                String[] filenameParts = requestedFile.getName().split("\\.");

                if (filenameParts.length > 1)
                {
                    filenameParts[filenameParts.length - 1] = extension;
                    String altFileName = String.join(".", filenameParts);

                    File imageFileGuess = new File(requestedFile.getParentFile(), altFileName);

                    log.info("Trying '{}'", imageFileGuess.getAbsolutePath());
                    if (imageFileGuess.exists())
                    {
                        log.info("Found!!");
                        return imageFileGuess;
                    }
                }
            }

            // Is it still not there?
            throw new FileNotFoundException(String.format("'%s' not found.", requestedFile.getPath()));
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
