/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.core;

/**
 * A monitor that fires callbacks at key points during the loading of an unstructured light field.
 * @author Michael Tetzlaff
 *
 */
public interface LoadingMonitor 
{
    /**
     * A callback fired when the unstructured light field starts to load.
     * This will always be fired before setProgress() or loadingComplete().
     */
    void startLoading();

    /**
     * A callback fired when the size of the total workload is determined.
     * setProgress() will never indicate a progress value higher than the maximum,
     * and when the progress value reaches the maximum, loadingComplete() should be fired shortly afterward.
     * @param maximum The maximum progress value.
     */
    void setMaximum(double maximum);

    /**
     * A callback fired when a progress checkpoint is reached.
     * This will always be fired between startLoading() and loadingComplete().
     * @param progress The amount of progress that has occurred.
     */
    void setProgress(double progress);

    /**
     * A callback fired when loading is complete.
     * This will always be fired after startLoading() and setProgress() will never be fired after this without startLoading() being fired once again.
     */
    void loadingComplete();

    /**
     * A callback fired when an exception occurs while loading.
     * @param e The exception that occurred.
     */
    void loadingFailed(Exception e);

    /**
     * A callback fired when an exception occurs while loading, but the process tries to recover and finish loading.
     * @param e The exception that occurred.
     */
    default void loadingWarning(Exception e)
    {
    }
}
