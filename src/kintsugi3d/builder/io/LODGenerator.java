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

import kintsugi3d.gl.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class LODGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(LODGenerator.class);

    private static final LODGenerator INSTANCE = new LODGenerator();

    public static LODGenerator getInstance()
    {
        return INSTANCE;
    }

    private LODGenerator()
    {
    }

    public void generateLODs(String format, int minResolution, File directory, String... originalImageFilenames)
    {
        for (String filename : originalImageFilenames)
        {
            generateLODs(format, minResolution, directory, filename);
        }
    }

    public void generateLODs(String format, int minResolution, File directory, String originalImageFilename)
    {
        File originalImageFile = new File(directory, originalImageFilename);

        try
        {
            if (originalImageFile.exists()) // Among other things, should catch when constant.png doesn't exist.
            {
                String filename = originalImageFile.getName();
                String extension = "";
                int i = filename.lastIndexOf('.'); //Strip file extension
                if (i > 0)
                {
                    extension = filename.substring(i);
                    filename = filename.substring(0, i);
                }

                ImageHelper imageHelper = ImageHelper.read(originalImageFile);

                for (int size = imageHelper.getBufferedImage().getHeight() / 2; size >= minResolution; size /= 2)
                {
                    imageHelper.saveAtResolution(format, new File(originalImageFile.getParent(), filename + "-" + size + extension), size);
                }
            }
        }
        catch (IOException e)
        {
            LOG.error("Error generating LODs for file '{}':", originalImageFile, e);
        }
    }
}
