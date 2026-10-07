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

package kintsugi3d.fx.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZoomSafeImageView
{
    private static final Logger LOG = LoggerFactory.getLogger(ZoomSafeImageView.class);

    private static final int MAX_CROP_SCREEN_PIXELS = Short.MAX_VALUE;

    private final ImageView imageView;

    private final ObjectProperty<Image> fullImage = new SimpleObjectProperty<>();
    private final ObjectProperty<Rectangle2D> logicalViewport = new SimpleObjectProperty<>();

    private Rectangle2D cropBounds;

    public ZoomSafeImageView(ImageView imageView)
    {
        this.imageView = imageView;

        fullImage.addListener(
            (observable, oldValue, newValue) -> refreshCroppedImage());

        logicalViewport.addListener(
            (observable, oldValue, newValue) ->
        {
            if (!cropBounds.contains(newValue)
                || getEffectiveRenderWidth() > MAX_CROP_SCREEN_PIXELS
                || getEffectiveRenderHeight() > MAX_CROP_SCREEN_PIXELS)
            {
                // Refresh the cropped image so that it can accommodate the new viewport.
                // This also refreshes the imageView viewport.
                refreshCroppedImage();
            }
            else
            {
                imageView.setViewport(new Rectangle2D(
                    newValue.getMinX() - cropBounds.getMinX(),
                    newValue.getMinY() - cropBounds.getMinY(),
                    newValue.getWidth(), newValue.getHeight()));
            }
        });
    }

    private double getEffectiveRenderWidth()
    {
        Rectangle2D currentViewport = getLogicalViewport();
        double viewportWidth = currentViewport != null ? currentViewport.getWidth() : fullImage.get().getWidth();
        return imageView.getImage().getWidth() * imageView.getLayoutBounds().getWidth() / viewportWidth;
    }

    private double getEffectiveRenderHeight()
    {
        Rectangle2D currentViewport = getLogicalViewport();
        double viewportHeight = currentViewport != null ? currentViewport.getHeight() : fullImage.get().getHeight();
        return imageView.getImage().getHeight() * imageView.getLayoutBounds().getHeight() / viewportHeight;
    }

    private void refreshCroppedImage()
    {
        Image currentFullImage = fullImage.get();
        if (currentFullImage == null)
        {
            // Reset if the image is null.
            imageView.setImage(null);
            imageView.setViewport(null);
            cropBounds = null;
        }
        else
        {
            int fullImageWidth = (int) currentFullImage.getWidth();
            int fullImageHeight = (int) currentFullImage.getHeight();

            Rectangle2D currentViewport = getLogicalViewport();

            // Make sure there's a valid viewport.
            if (currentViewport == null)
            {
                currentViewport = new Rectangle2D(0, 0, fullImageWidth, fullImageHeight);
            }

            Bounds imageViewBounds = imageView.getLayoutBounds();

            // Calculate the largest width and height that will not yield pixel coordinates greater than
            // MAX_CROP_SCREEN_PIXELS when scaled up to the on-screen resolution of the imageView.
            // Logical render resolution = cropWidth/widthRatio x cropHeight/heightRatio
            // i.e. if currentViewport were to match cropWidth x cropHeight, it would get scaled up to imageViewBounds
            //
            // We want to make sure that logical render resolution <= MAX_CROP_SCREEN_PIXELS
            // maxWidth/widthRatio = floor(MAX_CROP_SCREEN_PIXELS * widthRatio) / widthRatio <= MAX_CROP_SCREEN_PIXELS
            //
            // It is also important that current maxWidth >= viewport width.
            // This assumes that imageView width <= MAX_CROP_SCREEN_PIXELS (reasonable since monitor resolution typically <= 16K).
            // Thus MAX_CROP_SCREEN_PIXELS / imageView width > 1
            // maxWidth = floor(MAX_CROP_SCREEN_PIXELS * widthRatio)
            //  = floor(MAX_CROP_SCREEN_PIXELS * viewport width / imageView width)
            //  >= viewport width given the above constraints
            double widthRatio = currentViewport.getWidth() / Math.max(1.0, imageViewBounds.getWidth());
            double heightRatio = currentViewport.getHeight() / Math.max(1.0, imageViewBounds.getHeight());
            long maxWidth = (long)Math.floor(MAX_CROP_SCREEN_PIXELS * widthRatio);
            long maxHeight = (long)Math.floor(MAX_CROP_SCREEN_PIXELS * heightRatio);

            // Get safe viewport bounds for cropping, accounting for interpolation.
            int safeMinX = (int)Math.floor(currentViewport.getMinX());
            int safeMaxX = (int)Math.ceil(currentViewport.getMaxX());
            int safeMinY = (int)Math.floor(currentViewport.getMinY());
            int safeMaxY = (int)Math.ceil(currentViewport.getMaxY());

            // Divide by 2 so that we can zoom in a little without needing to refresh again.
            // Make sure that it always includes the full viewport and doesn't exceed the image width and height.
            int targetWidth = Math.max(Math.max(1, safeMaxX - safeMinX), (int)Math.min(fullImageWidth, maxWidth / 2));
            int targetHeight = Math.max(Math.max(1, safeMaxY - safeMinY), (int)Math.min(fullImageHeight, maxHeight / 2));

            // Calculate the current viewport center
            int currentCenterX = (int)Math.round(currentViewport.getMinX() + currentViewport.getWidth() * 0.5);
            int currentCenterY = (int)Math.round(currentViewport.getMinY() + currentViewport.getHeight() * 0.5);

            // Calculate the corner of the viewport based on the center and the determined height.
            // Make sure it includes the full viewport and does not go past (0, 0).
            int cropX = Math.max(0, Math.min(safeMinX, currentCenterX - targetWidth / 2));
            int cropY = Math.max(0, Math.min(safeMinY, currentCenterY - targetHeight / 2));

            // Make sure the cropped region does not go past the width and height of the image.
            // (Even though targetWidth and targetHeight will never exceed these bounds,
            // we still need to check this for when cropX and cropY are greater than zero.)
            if (cropX + targetWidth > fullImageWidth)
            {
                // Since targetWidth and targetHeight are already within the appropriate image bounds,
                // just adjust the start pixel to just extend how much is loaded before we need to refresh again.
                cropX = Math.max(0, fullImageWidth - targetWidth);
            }

            if (cropY + targetHeight > fullImageHeight)
            {
                cropY = Math.max(0, fullImageHeight - targetHeight);
            }

            // Copy pixels into a smaller image.
            WritableImage croppedImage = new WritableImage(targetWidth, targetHeight);
            croppedImage.getPixelWriter().setPixels(0, 0, targetWidth, targetHeight,
                currentFullImage.getPixelReader(), cropX, cropY);
            imageView.setViewport(null); // Prevent temporary inconsistent state.
            imageView.setImage(croppedImage);

            // Adjust the viewport and remember the crop bounds for later.
            imageView.setViewport(new Rectangle2D(
                currentViewport.getMinX() - cropX, currentViewport.getMinY() - cropY,
                currentViewport.getWidth(), currentViewport.getHeight()));
            cropBounds = new Rectangle2D(cropX, cropY, targetWidth, targetHeight);

            LOG.debug("Refreshed cropped image.");
        }
    }

    public Node getImageViewNode()
    {
        return imageView;
    }

    public Image getFullImage()
    {
        return fullImage.get();
    }

    public ObjectProperty<Image> fullImageProperty()
    {
        return fullImage;
    }

    public void setFullImage(Image fullImage)
    {
        this.fullImage.set(fullImage);
    }

    public Rectangle2D getLogicalViewport()
    {
        return logicalViewport.get();
    }

    public ObjectProperty<Rectangle2D> logicalViewportProperty()
    {
        return logicalViewport;
    }

    public void setLogicalViewport(Rectangle2D logicalViewport)
    {
        this.logicalViewport.set(logicalViewport);
    }

    public void setX(double value)
    {
        imageView.setX(value);
    }

    public double getX()
    {
        return imageView.getX();
    }

    public DoubleProperty xProperty()
    {
        return imageView.xProperty();
    }

    public void setY(double value)
    {
        imageView.setY(value);
    }

    public double getY()
    {
        return imageView.getY();
    }

    public DoubleProperty yProperty()
    {
        return imageView.yProperty();
    }

    public void setFitWidth(double value)
    {
        imageView.setFitWidth(value);
    }

    public double getFitWidth()
    {
        return imageView.getFitWidth();
    }

    public DoubleProperty fitWidthProperty()
    {
        return imageView.fitWidthProperty();
    }

    public void setFitHeight(double value)
    {
        imageView.setFitHeight(value);
    }

    public double getFitHeight()
    {
        return imageView.getFitHeight();
    }

    public DoubleProperty fitHeightProperty()
    {
        return imageView.fitHeightProperty();
    }

    public void setPreserveRatio(boolean value)
    {
        imageView.setPreserveRatio(value);
    }

    public boolean isPreserveRatio()
    {
        return imageView.isPreserveRatio();
    }

    public BooleanProperty preserveRatioProperty()
    {
        return imageView.preserveRatioProperty();
    }

    public void setSmooth(boolean value)
    {
        imageView.setSmooth(value);
    }

    public boolean isSmooth()
    {
        return imageView.isSmooth();
    }

    public BooleanProperty smoothProperty()
    {
        return imageView.smoothProperty();
    }
}
