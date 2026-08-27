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

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

class ImageSelectionPreview
{
    private final ImageView imageView;
    private final Text imageViewText;
    private final ImageSelectionCache cache;

    ImageSelectionPreview(ImageView imageView, Text imageViewText, ImageSelectionCache cache)
    {
        this.imageView = imageView;
        this.imageViewText = imageViewText;
        this.cache = cache;
    }

    public ImageView getImageView()
    {
        return imageView;
    }

    public String getImageViewText()
    {
        return imageViewText.getText();
    }

    public void setImageViewText(String txt)
    {
        imageViewText.setText(txt);
    }

    public Image retrieveFromCache(String imageName)
    {
        return cache.retrieveFromCache(imageName);
    }

    public void addToCache(String imageName, Image image)
    {

        // If the image is already in the cache, then remove it from the queue
        // and put it in the back of the queue as it was just accessed.
        // Otherwise, just put it in the back of the queue.

        // Reduce cache size as needed.
        cache.addToCache(imageName, image);
    }
}
