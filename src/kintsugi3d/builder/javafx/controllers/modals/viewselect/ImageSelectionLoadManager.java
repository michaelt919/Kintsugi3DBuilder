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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

class ImageSelectionLoadManager
{
    private static final Logger LOG = LoggerFactory.getLogger(ImageSelectionLoadManager.class);

    private final Function<String, ImageSelectionLoader> imageLoaderFactory;

    private ImageSelectionLoader runningImageLoader;
    private ImageSelectionLoader queuedImageLoader;
    private Thread runningThread;
    private Thread queuedThread;
    private final Object imageLoaderLock = new Object();

    ImageSelectionLoadManager(Function<String, ImageSelectionLoader> imageLoaderFactory)
    {
        this.imageLoaderFactory = imageLoaderFactory;
    }

    void loadImage(String imageName)
    {
        ImageSelectionLoader newImageLoader = imageLoaderFactory.apply(imageName);

        synchronized (imageLoaderLock)
        {
            queuedImageLoader = newImageLoader;
            queuedThread = new Thread(() ->
            {
                try
                {
                    Thread threadToJoin;

                    synchronized (imageLoaderLock)
                    {
                        // Grab the reference in case another thread changes what runningThread references.
                        threadToJoin = runningThread;
                    }

                    if (threadToJoin != null)
                    {
                        // Wait for the thread to finish to avoid having multiple threads running at the same time.
                        threadToJoin.join();
                    }
                }
                catch (InterruptedException e)
                {
                    // If interrupted just keep going and let the previous thread run in the background until it hits a stop check.
                    LOG.debug("Interrupted while waiting for image loader to finish", e);
                }

                Thread thisThread = Thread.currentThread();

                synchronized (imageLoaderLock)
                {
                    // Update running / queued in a synchronized block to ensure atomicity.
                    // By contract, we must set queuedThread to null to maintain an invariant that any
                    // queuedThread must not have started running its associated queuedImageLoader.
                    runningThread = thisThread;
                    runningImageLoader = newImageLoader;
                    queuedThread = null;
                    queuedImageLoader = null;
                }

                newImageLoader.run();

            }, "Image Selection Loader");

            queuedThread.start();
        }
    }

    void cancelLoad()
    {
        Thread queuedThreadToJoin = null;

        synchronized (imageLoaderLock)
        {
            // If loadImgThread is running, kill it and start a new one
            if (runningImageLoader != null)
            {
                runningImageLoader.stopThread();
            }

            if (queuedImageLoader != null)
            {
                // Stop any previously queued image loader before it starts
                queuedImageLoader.stopThread();

                // Force the queued thread to stop waiting for a thread to finish.
                // This will wake i[ the queued thread from its own join if applicable.
                // By contract, whatever thread is assigned to queuedThread will not have actually started running
                // its associated queuedImageLoader, so it should terminate immediately and avoid race conditions.
                queuedThread.interrupt();

                // Grab a reference now so that we can leave the synchronized block (and avoid deadlock)
                // but also prevent a race condition if the queued thread
                queuedThreadToJoin = queuedThread;
            }
        }

        if (queuedThreadToJoin != null)
        {
            try
            {
                // Join outside the synchronized block to avoid deadlock.
                // Should return almost immediately since the queued thread has been interrupted
                // and a stop has been requested.
                // Since this code runs sequentially on the JavaFX thread,
                // this system should ensure that at any given time only two managed threads are running:
                // the "running" thread and the "queued" thread.
                // To start another thread, we need to cancel and join with any pre-existing "queued" thread first.
                // In rare cases when an InterruptedException is thrown, another thread may continue running
                // but it should have still been signaled to stop to prevent logical race conditions.
                queuedThreadToJoin.join();

                // At this point queuedThread and queuedImageLoader should both be null
                // as a result of the queued thread finishing.
            }
            catch (InterruptedException e)
            {
                LOG.debug("Interrupted while joining with a cancelled image loader", e);
            }
        }
    }
}