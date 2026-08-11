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

package kintsugi3d.builder.javafx.internal;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.state.CacheModelBase;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class ObservableCacheModel extends CacheModelBase
{
    private static final Logger LOG = LoggerFactory.getLogger(ObservableCacheModel.class);

    /**
     * This should ONLY be modified in one place, via the requestCacheSizeRefresh method.
     * Otherwise, there would be bad race conditions.
     */
    private final LongProperty cacheSize = new SimpleLongProperty(-1); // -1 signifies uninitialized;

    private final DoubleBinding cacheSizeGB = cacheSize.multiply(BYTES_TO_GB);

    /**
     * This should ONLY be modified in one place, via the requestCacheSizeRefresh method.
     * Otherwise, there would be bad race conditions.
     * The intention is for this to just be used for UI display purposes, not internal thread synchronization logic.
     */
    private final BooleanProperty cacheSizeCalcInProgress = new SimpleBooleanProperty(false);

    /**
     * Use a separate property for handling one-shot listeners
     * that we can dispose to dump in case the listener doesn't need to fire.
     * Important: should only be accessed in blocks synchronized on CACHE_SIZE_CALC_THREAD_LOCK
     * to prevent concurrent modification issues.
     */
    private final Collection<ChangeListener<Number>> pendingCacheSizeCallbacks = new ArrayList<>(1);

    private Map<String, Long> projectSizes = Map.of();

    @Override
    public long getCacheSizeBytes()
    {
        return cacheSize.get();
    }

    @Override
    public Map<String, Long> getProjectSizes()
    {
        // Should already be unmodifiable.
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return projectSizes;
    }

    public DoubleBinding getCacheSizeGBProperty()
    {
        return cacheSizeGB;
    }

    @Override
    public double getCacheSizeGB()
    {
        return cacheSizeGB.get();
    }

    public BooleanProperty getCacheSizeCalcInProgressProperty()
    {
        return cacheSizeCalcInProgress;
    }

    @Override
    public boolean isCacheSizeCalcInProgress()
    {
        return cacheSizeCalcInProgress.get();
    }

    @Override
    protected void setCacheSizeCalcInProgress(boolean cacheSizeCalcInProgress)
    {
        this.cacheSizeCalcInProgress.set(cacheSizeCalcInProgress);
    }

    @Override
    protected void updateCacheSize(long newCacheSize, Map<String, Long> newProjectSizes, Runnable onCompleteCallback)
    {
        Platform.runLater(() ->
        {
            try
            {
                synchronizedCacheSizeCalcComplete(() ->
                {
                    this.projectSizes = Collections.unmodifiableMap(newProjectSizes);

                    // This will trigger any listeners attached to the property.
                    this.cacheSize.set(newCacheSize);
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
        });
    }

    @Override
    protected void addCacheSizeChangeCallback(DoubleConsumer cacheSizeChangeCallback)
    {
        // Add one-shot listener with the specified callback before checking whether calculation is in progress.
        // Because cacheSize should only be modified in one place (within a Platform.runLater fired by the worker thread),
        // synchronization will ensure that any update will not be queued (and thus not fired)
        // until after we exit this synchronized block.
        // This works whether we need to start the thread or one is already running.
        ChangeListener<Number> changeListener = new ChangeListener<>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                cacheSizeGB.removeListener(this); // Remove first in case the callback throws an exception.
                pendingCacheSizeCallbacks.remove(this);

                if (cacheSizeChangeCallback != null)
                {
                    try
                    {
                        cacheSizeChangeCallback.accept(newValue.doubleValue());
                    }
                    catch (RuntimeException e)
                    {
                        LOG.error("Exception thrown by cache size callback", e);
                    }
                }
            }
        };

        pendingCacheSizeCallbacks.add(changeListener); // add to our list first to avoid race conditions with JavaFX Application Thread
        cacheSizeGB.addListener(changeListener);
    }

    @Override
    public void removeAllCacheSizeChangeCallbacks()
    {
        pendingCacheSizeCallbacks.forEach(cacheSizeGB::removeListener);
        pendingCacheSizeCallbacks.clear();
    }

    public void clearCachePrompt()
    {
        Map<File, Consumer<File[]>> deleteMethods = getDeleteMethods();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cache");
        confirm.setHeaderText("Confirm cache clear?");
        confirm.setContentText(String.format("This will permanently remove all files in %s, including the ones for your current project, and cannot be undone.  Are you sure?",
            String.join(" and ", deleteMethods.keySet().stream().map(File::toString).toArray(String[]::new))));

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                requestClearCache();
            }
        });
    }

    public void checkForCleanUpCachePrompt()
    {
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("recentPromptEnabled") || settingsModel.getBoolean("fileAgePromptEnabled")
            || settingsModel.getBoolean("sizePromptEnabled"))
        {
            cleanUpBothCachesPrompt();
        }
    }

    private void cleanUpBothCachesPrompt()
    {
        Map<File, Consumer<File[]>> deleteMethods = getDeleteMethods();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Old Cache Files");
        confirm.setHeaderText("Confirm cache clean up?");

        confirm.setContentText(String.format("This will permanently remove files in %s, and cannot be undone.  Are you sure?",
            String.join(" and ", deleteMethods.keySet().stream().map(File::toString).toArray(String[]::new))));

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                requestCleanUpBothCaches(deleteMethods);
            }
        });
    }

    /**
     * Check if any cache cleanup conditions are enabled and triggered.
     * If so, a callback will be fired with the current cache size in GB.
     * If no enabled cache cleanup conditions are met, this method does nothing, although it may still trigger
     * calculation of the cache size if said calculation has not yet been performed.
     * If the cache size needs to be calculated, the cleanup conditions will be
     * checked asynchronously in a worker thread that will also store the cache size.
     * Otherwise, the conditions will be checked immediately.
     * @param promptWithCacheSizeGB The callback, which takes as input the current cache size in GB.
     */
    public void requestPromptForCacheCleanup(
        Consumer<Double> promptWithCacheSizeGB, Consumer<Double> noCleanupNeededWithCacheSizeGB)
    {
        if (getCacheSizeBytes() < 0)
        {
            // Cache size needs to be calculated.
            requestCacheSizeRefresh(newCacheSize ->
                checkforCleanupPrompts(newCacheSize, promptWithCacheSizeGB, noCleanupNeededWithCacheSizeGB),
                null);
        }
        else
        {
            // Cache size is already calculated, just show the prompt.
            checkforCleanupPrompts(getCacheSizeGB(), promptWithCacheSizeGB, noCleanupNeededWithCacheSizeGB);
        }
    }

    private void checkforCleanupPrompts(double newCacheSizeGB, Consumer<Double> promptWithCacheSizeGB,
                                               Consumer<Double> noCleanupNeededWithCacheSizeGB)
    {
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("sizePromptEnabled"))
        {
            if (newCacheSizeGB > settingsModel.getFloat("cacheSizeLimit"))
            {
                promptWithCacheSizeGB.accept(newCacheSizeGB);
            }
        }
        if (settingsModel.getBoolean("recentPromptEnabled"))
        {
            if (getNumCachedProjects() > settingsModel.getInt("recentProjectLimit"))
            {
                promptWithCacheSizeGB.accept(newCacheSizeGB);
            }
        }
        if (settingsModel.getBoolean("fileAgePromptEnabled"))
        {
            if (checkOldFilesExist())
            {
                promptWithCacheSizeGB.accept(newCacheSizeGB);
            }
        }

        // If the cache does not need to be cleaned up, then fire the "no cleanup needed" callback.
        noCleanupNeededWithCacheSizeGB.accept(newCacheSizeGB);
    }

    @Override
    protected void handleCacheCleanupError(Exception e)
    {
        super.handleCacheCleanupError(e);
        ExceptionHandling.error("An error occurred while cleaning up cache.  Consider deleting cache files manually.", e);
    }
}
