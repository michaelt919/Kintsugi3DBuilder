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

package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.util.ImageThreadable;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

class ImageSelectionThread implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(ImageSelectionThread.class);
    private final String imageName;
    private final ViewSelectionModel model;
    private final ImageThreadable imageThreadable;
    private volatile boolean stopRequested = false;
    private volatile boolean isRunning = false;

    ImageSelectionThread(String imageName, ImageThreadable imgThreadable, ViewSelectionModel model)
    {
        this.imageName = imageName;
        this.imageThreadable = imgThreadable;
        this.model = model;
    }

    @Override
    public void run()
    {
        isRunning = true;
        loadFullResImg(imageName);
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

    private void loadFullResImg(String imageName)
    {
        ImageView imgView = imageThreadable.getImageView();
        Map<String, Image> imgCache = imageThreadable.getImageCache();

        Image image = null; //use cached img if possible
        if (imgCache.containsKey(imageName))
        {
            image = imgCache.get(imageName);
        }
        else
        {
            try
            {
                String path = model.findFullResImagePath(imageName).orElse("");

                File imgFile;
                try
                {
                    imgFile = ImageFinder.getInstance().findImageFile(new File(path));
                }
                catch (FileNotFoundException ignored)
                {
                    imageThreadable.setImageViewText(imageThreadable.getImageViewText() +
                        " (full res image not found)");
                    return;
                }

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

                if (stopRequested)
                {
                    return;
                }

                imgCache.put(imageName, image);
            }
            catch (IllegalArgumentException e)
            {
                // could not find image
                imageThreadable.setImageViewText(
                    imageThreadable.getImageViewText() + " (full res image not found)");
                LOG.warn("Could not find full res image", e);
            }
            catch (IOException e)
            {
                LOG.warn("Failed to read image", e);
            }
            catch (Exception e)
            {
                LOG.warn("Image selection thread failed to find " + imageName, e);
            }
        }

        Image finalImage = image; // copy here so a final version of image can be passed to lambda expression
        if (finalImage != null && !stopRequested)
        {
            Platform.runLater(() ->
            {
                imgView.setImage(finalImage);
                imageThreadable.setImageViewText(imageName);
            });
        }
    }
}
