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

package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

class ImageSelectionLoader implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(ImageSelectionLoader.class);
    private final String imageName;
    private final ViewSelectionModel model;
    private final ImageSelectionPreview preview;
    private volatile boolean stopRequested = false;
    private volatile boolean isRunning = false;

    ImageSelectionLoader(String imageName, ImageSelectionPreview preview, ViewSelectionModel model)
    {
        this.imageName = imageName;
        this.preview = preview;
        this.model = model;
    }

    @Override
    public void run()
    {
        isRunning = true;
        if (!stopRequested)
        {
            try
            {
                loadFullResImg();
            }
            catch (FileNotFoundException|RuntimeException e)
            {
                preview.setImageViewText(
                    String.format("%s (full res image not found)", preview.getImageViewText()));
                return;
            }
        }
        isRunning = false;
    }

    public boolean isActive()
    {
        return isRunning;
    }

    public void stopThread()
    {
        stopRequested = true;
    }

    private void loadFullResImg() throws FileNotFoundException
    {
        ImageView imgView = preview.getImageView();
        Image image = preview.retrieveFromCache(imageName); //use cached img if possible
        if (image == null)
        {
            try
            {
                String path = model.findFullResImagePath(imageName).orElse("");
                File imgFile = ImageFinder.getInstance().findImageFile(new File(path));

                if (stopRequested)
                {
                    return;
                }

                // load image and masks if present
                BufferedImage bufferedImage = ImageHelper.read(imgFile).getBufferedImage();

                if (stopRequested)
                {
                    return;
                }

                image = SwingFXUtils.toFXImage(bufferedImage, null);

                // Even if stop requested, at this point we should still put the image in the cache as it might be needed later.
                preview.addToCache(imageName, image);
            }
            catch (IOException e)
            {
                LOG.warn("Failed to read image", e);
            }
            catch (RuntimeException e)
            {
                LOG.warn("Image selection thread failed to find {}", imageName, e);
            }
        }

        Image finalImage = image; // copy here so a final version of image can be passed to lambda expression
        if (finalImage != null && !stopRequested)
        {
            Platform.runLater(() ->
            {
                // Need to check for stopRequested one more time.
                // Since this runs on the JavaFX thread, we shouldn't have race conditions with another image load starting
                // (since that also runs on the JavaFX thread initially).
                if (!stopRequested)
                {
                    imgView.setImage(finalImage);
                    preview.setImageViewText(imageName);
                }
            });
        }
    }
}
