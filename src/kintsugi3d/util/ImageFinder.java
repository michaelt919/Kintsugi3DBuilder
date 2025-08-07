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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Singleton class
 */
public final class ImageFinder
{
    private static final Logger log = LoggerFactory.getLogger(ImageFinder.class);

    private static final ImageFinder INSTANCE = new ImageFinder();
    private static final Set<String> IMG_FORMATS = Set.of( "png", "PNG", "jpg", "JPG", "jpeg", "JPEG", "tif", "TIF", "tiff", "TIFF");

    public static ImageFinder getInstance()
    {
        return INSTANCE;
    }

    private ImageFinder()
    {
    }

    public Set<String> getSupportedImgFormats()
    {
        return IMG_FORMATS;
    }

    /**
     * Gets a modified copy of the name of an image file with a specific file extension (i.e. PNG, JPEG, etc.)
     * -- which may not match the format originally specified in the original filename.
     * If the original file format is one of the formats returned by getSupportedImgFormats(),
     * the old file extension will be replaced by the new one.
     * Otherwise, the file extension will be appended to avoid corrupting file names that include dots/periods
     * separating parts of the filename other than the file extension (a common practice in some naming conventions).
     * @param  imageFileName The original filename
     * @param format The desired format
     * @return The image file's name with the requested format.
     */
    public String getImageFileNameWithFormat(String imageFileName, String format)
    {
        if (imageFileName.endsWith(format))
        {
            // Filename already is in the requested format.
            return imageFileName;
        }
        else
        {
            String[] parts = imageFileName.split("\\.");

            if(IMG_FORMATS.contains(parts[parts.length - 1]))
            {
                // Replace the old file extension with the new one if recognized.
                return Stream.concat(Arrays.stream(parts, 0, Math.max(1, parts.length - 1)), Stream.of(format))
                    .collect(Collectors.joining("."));
            }
            else
            {
                // Otherwise just append the  new file extension to the end (even if the filename appears to have an extension,
                // it may just be a name with dots in it that already had the standard extension stripped).
                return String.format("%s.%s", imageFileName, format);
            }
        }
    }

    private static void logFileGuess(File imageFileGuess)
    {
        log.info("Trying '{}'", imageFileGuess.getAbsolutePath());
    }

    private static void logFound()
    {
        log.info("Found!!");
    }

    public File findImageFile(File requestedFile, String... suffixes) throws FileNotFoundException
    {
        if (requestedFile.exists())
        {
            return requestedFile;
        }
        else
        {
            // Try some alternate file formats/extensions
            // Try appending first (will catch filenames that contain .'s but omit the extension)
            File parentFile = requestedFile.getParentFile();

            for(String extension : IMG_FORMATS)
            {
                String altFileName = String.join(".", requestedFile.getName(), extension);
                File imageFileGuess = new File(parentFile, altFileName);

                logFileGuess(imageFileGuess);
                if (imageFileGuess.exists())
                {
                    logFound();
                    return imageFileGuess;
                }

                if (suffixes != null)
                {
                    for (String suffix : suffixes)
                    {
                        altFileName = String.join(".", String.format("%s%s", requestedFile.getName(), suffix), extension);
                        imageFileGuess = new File(parentFile, altFileName);

                        logFileGuess(imageFileGuess);
                        if (imageFileGuess.exists())
                        {
                            logFound();
                            return imageFileGuess;
                        }
                    }
                }
            }

            // try substituting the part after the last . with various extensions
            String[] filenameParts = requestedFile.getName().split("\\.");
            if (filenameParts.length > 1)
            {
                String originalEnding = filenameParts[filenameParts.length - 2];

                for(String extension : IMG_FORMATS)
                {
                    filenameParts[filenameParts.length - 1] = extension;
                    filenameParts[filenameParts.length - 2] = originalEnding;
                    String altFileName = String.join(".", filenameParts);

                    File imageFileGuess = new File(parentFile, altFileName);

                    logFileGuess(imageFileGuess);
                    if (imageFileGuess.exists())
                    {
                        logFound();
                        return imageFileGuess;
                    }

                    if (suffixes != null)
                    {
                        for (String suffix : suffixes)
                        {
                            filenameParts[filenameParts.length - 2] = String.format("%s%s", originalEnding, suffix);
                            altFileName = String.join(".", filenameParts);

                            imageFileGuess = new File(parentFile, altFileName);

                            logFileGuess(imageFileGuess);
                            if (imageFileGuess.exists())
                            {
                                logFound();
                                return imageFileGuess;
                            }
                        }
                    }
                }
            }

            // Is it still not there?
            throw new FileNotFoundException(String.format("'%s' not found.", requestedFile.getPath()));
        }
    }

    public File findImageFile(File requestedFile) throws FileNotFoundException
    {
        return findImageFile(requestedFile, (String[]) null);
    }

    public File tryFindImageFile(File requestedFile, String... suffixes)
    {
        try
        {
            return findImageFile(requestedFile, suffixes);
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
    }

    public File tryFindImageFile(File requestedFile)
    {
        return tryFindImageFile(requestedFile, (String[]) null);
    }
}
