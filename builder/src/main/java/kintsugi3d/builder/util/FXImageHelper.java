/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import kintsugi3d.gl.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

public final class FXImageHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(FXImageHelper.class);
    private static final Pattern TIFF_PATTERN = Pattern.compile(".*\\.tiff?");

    private FXImageHelper()
    {
    }

    public static Image loadFXImage(File imageFile)
    {
        return loadFXImage(new FileSource(imageFile), imageFile.getAbsolutePath());
    }

    public static Image loadFXImage(InputStream imageStream, String imageFileName)
    {
        return loadFXImage(new StreamSource(imageStream), imageFileName);
    }

    private static Image loadFXImage(Source source, String imageFileName)
    {
        // convert tiff image if necessary
        if (TIFF_PATTERN.matcher(imageFileName.toLowerCase(Locale.ROOT)).matches())
        {
            try
            {
                BufferedImage bufferedImage = source.getImageHelper().getBufferedImage();
                return SwingFXUtils.toFXImage(bufferedImage, null);
            }
            catch (IOException e)
            {
                LOG.error("Could not convert tiff image: ", e);
                return null;
            }
        }
        else
        {
            return source.getStandardImage();
        }
    }

    private interface Source
    {
        /**
         * Usable for image types supported by JavaFX (PNG, JPEG)
         * @return
         */
        Image getStandardImage();

        /**
         * Necessary for image types not supported by JavaFX (i.e. TIFF)
         * @return
         * @throws IOException
         */
        ImageHelper getImageHelper() throws IOException;
    }

    private static class FileSource implements Source
    {
        private final File file;

        FileSource(File file)
        {
            this.file = file;
        }

        /**
         * Usable for image types supported by JavaFX (PNG, JPEG)
         *
         * @return
         */
        @Override
        public Image getStandardImage()
        {
            return new Image(file.toURI().toString());
        }

        /**
         * Necessary for image types not supported by JavaFX (i.e. TIFF)
         *
         * @return
         * @throws IOException
         */
        @Override
        public ImageHelper getImageHelper() throws IOException
        {
            return ImageHelper.read(file);
        }
    }

    private static class StreamSource implements Source
    {
        private final InputStream inputStream;

        StreamSource(InputStream inputStream)
        {
            this.inputStream = inputStream;
        }

        /**
         * Usable for image types supported by JavaFX (PNG, JPEG)
         *
         * @return
         */
        @Override
        public Image getStandardImage()
        {
            return new Image(inputStream);
        }

        /**
         * Necessary for image types not supported by JavaFX (i.e. TIFF)
         *
         * @return
         * @throws IOException
         */
        @Override
        public ImageHelper getImageHelper() throws IOException
        {
            return ImageHelper.read(inputStream);
        }
    }
}
