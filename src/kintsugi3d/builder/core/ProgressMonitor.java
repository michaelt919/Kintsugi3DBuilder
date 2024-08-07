/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;

/**
 * A monitor that fires callbacks at key points during the loading of an unstructured light field.
 * @author Michael Tetzlaff
 *
 */
public interface ProgressMonitor
{
    /**
     * This method will be called at points when it is possible to cancel the process without unpredictable results.
     * If it returns true, the process will be cancelled.
     * @return true if cancellation is requested; false otherwise.
     */
    void allowUserCancellation() throws UserCancellationException;

    /**
     * This method will be called if user cancellation occurred to indicate that cancellation is complete
     * (usually caused by the UserCancellationException percolating up the call stack to a point outside the operation that was cancelled).
     * @param e The UserCancellationException indicating the stack trace where the operation was cancelled.
     */
    void cancelComplete(UserCancellationException e);

    /**
     * A callback fired when the unstructured light field starts to load.
     * This will always be fired before setProgress() or complete().
     */
    void start();

    /**
     * Send the name of the process up the call stack to the title of the Progress Bars modal.
     * @param processName The name of the process.
     */
    void setProcessName(String processName);

    /**
     * A callback fired when the total number of stages is known.
     * setStageComplete() will never indicate a number of completed stages greater than this,
     * and when the number of completed stages reaches the total count, complete() should be fired shortly afterward.
     * @param count The number of stages.
     */
    void setStageCount(int count);

    /**
     * A callback fired when the current stage changes.
     * This will always be fired between startLoading() and loadingComplete().
     * @param stage The number of stages that have been completed.
     * @param message Text describing the next stage of the process.
     */
    void setStage(int stage, String message);

    /**
     * A callback fired when the size of the total workload is determined.
     * setProgress() will never indicate a progress value higher than the maximum,
     * and when the progress value reaches the maximum, setStageComplete() should be fired shortly afterward.
     * @param maxProgress The maximum progress value.
     */
    void setMaxProgress(double maxProgress);

    /**
     * A callback fired when a progress checkpoint is reached.
     * This will always be fired between start() and complete().
     * @param progress The amount of progress that has occurred.
     * @param message Text describing the current step of the current stage.
     */
    void setProgress(double progress, String message);

    /**
     * A callback fired when the operation is complete.
     * This will always be fired after start() and setProgress() will never be fired after this without start() being fired once again.
     */
    void complete();

    /**
     * A callback fired when an exception occurs.
     * @param e The exception that occurred.
     */
    void fail(Throwable e);

    /**
     * A callback fired when an exception occurs, but the operation tries to recover and finish.
     * @param e The exception that occurred.
     */
    default void warn(Throwable e)
    {
    }

    /**
     * Usually a call to ProgressBarsController.getInstance().isProcessing().
     * Used to stop multiple processes from conflicting with each other.
     * @return true if a process is in progress.
     */
    boolean isConflictingProcess();
}
