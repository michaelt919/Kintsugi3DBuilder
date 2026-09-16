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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

class ImageSelectionCache
{
    private static final int MAX_IMAGE_CACHE_QUEUE_SIZE = 16;

    private final Map<String, Image> imageCache;
    private final Queue<String> imageCacheRemovalQueue;

    ImageSelectionCache()
    {
        this.imageCache = new HashMap<>(MAX_IMAGE_CACHE_QUEUE_SIZE);
        this.imageCacheRemovalQueue = new ArrayDeque<>(MAX_IMAGE_CACHE_QUEUE_SIZE);
    }

    public boolean contains(String imageName)
    {
        return imageCache.containsKey(imageName);
    }

    public Image retrieveFromCache(String imageName)
    {
        Image image = imageCache.get(imageName);
        if (image != null)
        {
            // If the image is already in the cache, then remove it from the queue
            // and put it in the back of the queue as it was just accessed.
            imageCacheRemovalQueue.remove(imageName);
            imageCacheRemovalQueue.add(imageName);

            return image;
        }
        else
        {
            return null;
        }
    }

    public void addToCache(String imageName, Image image)
    {
        imageCache.put(imageName, image);

        // If the image is already in the cache, then remove it from the queue
        // and put it in the back of the queue as it was just accessed.
        // Otherwise, just put it in the back of the queue.
        imageCacheRemovalQueue.remove(imageName);
        imageCacheRemovalQueue.add(imageName);

        // Reduce cache size as needed.
        if (imageCache.size() > MAX_IMAGE_CACHE_QUEUE_SIZE)
        {
            imageCache.remove(imageCacheRemovalQueue.poll());
        }
    }
}