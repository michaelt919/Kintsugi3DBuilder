/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Kaelie Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday, Joseph O'Connor
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.app.test;

import kintsugi3d.builder.state.CacheModelBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.DoubleConsumer;

public class TestingCacheModel extends CacheModelBase
{
    private static final Logger LOG = LoggerFactory.getLogger(TestingCacheModel.class);

    private long cacheSize;
    private Map<String, Long> projectSizes;
    private boolean cacheSizeCalcInProgress;

    private final List<DoubleConsumer> cacheSizeChangedCallbacks = new ArrayList<>(1);

    @Override
    public long getCacheSizeBytes()
    {
        return cacheSize;
    }

    @Override
    public double getCacheSizeGB()
    {
        return cacheSize * BYTES_TO_GB;
    }

    @Override
    public Map<String, Long> getProjectSizes()
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return projectSizes;
    }

    @Override
    public boolean isCacheSizeCalcInProgress()
    {
        return cacheSizeCalcInProgress;
    }

    @Override
    protected void updateCacheSize(long newCacheSize, Map<String, Long> newProjectSizes, Runnable onCompleteCallback)
    {
        try
        {
            synchronizedCacheSizeCalcComplete(() ->
            {
                this.projectSizes = Collections.unmodifiableMap(newProjectSizes);
                this.cacheSize = newCacheSize;

                Collection<DoubleConsumer> callbacks;
                synchronized (cacheSizeChangedCallbacks)
                {
                    // Copy the callback list for synchronization
                    callbacks = new ArrayList<>(cacheSizeChangedCallbacks);

                    // Make sure callbacks aren't called twice.
                    cacheSizeChangedCallbacks.clear();
                }

                // Run all the callbacks
                for (DoubleConsumer callback : callbacks)
                {
                    try
                    {
                        callback.accept(getCacheSizeGB());
                    }
                    catch (RuntimeException e)
                    {
                        LOG.error("Exception thrown by cache size callback", e);
                    }
                }
            });
        }
        finally // Another finally block that isn't synchronized to avoid blocking other threads.
        {
            // Callback that should always run regardless of whether the cache size changed or not.
            if (onCompleteCallback != null)
            {
                onCompleteCallback.run();
            }
        }
    }

    @Override
    protected void setCacheSizeCalcInProgress(boolean cacheSizeCalcInProgress)
    {
        this.cacheSizeCalcInProgress = cacheSizeCalcInProgress;
    }

    @Override
    protected void addCacheSizeChangeCallback(DoubleConsumer cacheSizeChangeCallback)
    {
        if (cacheSizeChangeCallback != null)
        {
            synchronized (cacheSizeChangedCallbacks)
            {
                cacheSizeChangedCallbacks.add(cacheSizeChangeCallback);
            }
        }
    }

    @Override
    protected void removeAllCacheSizeChangeCallbacks()
    {
        synchronized (cacheSizeChangedCallbacks)
        {
            cacheSizeChangedCallbacks.clear();
        }
    }
}
