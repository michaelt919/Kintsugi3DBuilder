/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.interactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class RefreshableCollection<RefreshableType extends Refreshable> implements Refreshable
{
    private static final Logger LOG = LoggerFactory.getLogger(RefreshableCollection.class);

    private final Collection<RefreshableType> refreshables = new ArrayList<>(8);
    private final Collection<RefreshableType> initializeQueue = new ArrayList<>(8);
    private final Collection<RefreshableType> terminateQueue = new ArrayList<>(8);

    private boolean initialized = false;

    public void add(RefreshableType refreshable)
    {
        initializeQueue.add(refreshable);
    }

    public void remove(RefreshableType refreshable)
    {
        terminateQueue.add(refreshable);
    }

    public void clear()
    {
        terminateQueue.addAll(refreshables);
    }

    private void processInitializeQueue()
    {
        for (RefreshableType r : initializeQueue)
        {
            // Initialize added refreshables and add to main list.
            try
            {
                if (!r.isInitialized()) // Skip if it's already initialized
                {
                    // Run initialize method which might throw an exception
                    r.initialize();
                }

                // Add to main list if initialization was successful.
                refreshables.add(r);
            }
            catch (InitializationException|RuntimeException e)
            {
                LOG.error("Error initializing refreshable object", e);
            }
        }

        // Clear initialize queue after processing.
        initializeQueue.clear();
    }

    private void processTerminateQueue()
    {
        for (RefreshableType r : terminateQueue)
        {
            // Remove from main refreshable list first in case exception is thrown --
            // want it to be no longer processed regardless.
            refreshables.remove(r);

            try
            {
                // Terminate.  If an exception happens, termination will not be attempted again,
                // but the refreshable will no longer be processed in the main loop.
                r.terminate();
            }
            catch (RuntimeException e)
            {
                LOG.error("Error terminating refreshable object", e);
            }
        }

        // Clear terminate queue after processing.
        terminateQueue.clear();
    }

    @Override
    public boolean isInitialized()
    {
        return this.initialized;
    }

    @Override
    public void initialize()
    {
        // Edge case for if refreshable was added and removed prior to initialize()
        initializeQueue.removeAll(terminateQueue);
        terminateQueue.clear();

        // Initialize added refreshables and add to main list.
        processInitializeQueue();

        this.initialized = true;
    }

    @Override
    public void refresh()
    {
        // Process initialize and terminate queues.
        processInitializeQueue();
        processTerminateQueue();

        for (Refreshable r : refreshables)
        {
            r.refresh();
        }
    }

    @Override
    public void terminate()
    {
        processTerminateQueue();

        // Empty out everything to leave it in a clean state.
        initializeQueue.clear();
        refreshables.clear();
    }
}
