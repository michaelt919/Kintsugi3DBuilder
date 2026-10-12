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

package kintsugi3d.builder.io;

import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.gl.interactive.UserCancellationException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Handles cancellation requests that come from the backend, such as when closing a project triggers cancellation.
 */
final class CancellationMonitor implements ProgressMonitor
{
    private boolean running = false;
    private boolean cancelRequested = false;
    private final Collection<Runnable> cancelCallbacks = new ArrayList<>(1);
    private final Object lock = new Object();

    /**
     *
     * @param cancelCallback Guaranteed to run as soon as there are no running tasks
     *                       (even if no tasks are currently running when this method is called).
     */
    public void requestCancellation(Runnable cancelCallback)
    {
        synchronized (lock)
        {
            if (running)
            {
                cancelCallbacks.add(cancelCallback);
                cancelRequested = true;
                return;
            }
        }

        // If we didn't return within the synchronized block, then it wasn't running so we can just run the cancel callback now.
        cancelCallback.run();
    }

    /**
     * This method will be called at points when it is possible to cancel the process without unpredictable results.
     * If it returns true, the process will be cancelled.
     *
     * @return true if cancellation is requested; false otherwise.
     */
    @Override
    public void allowUserCancellation() throws UserCancellationException
    {
        synchronized (lock)
        {
            if (cancelRequested)
            {
                cancelRequested = false;
                throw new UserCancellationException();
            }
        }
    }

    @Override
    public void cancelComplete(UserCancellationException e)
    {
        runCancelCallbacks();
    }

    private void runCancelCallbacks()
    {
        Collection<Runnable> cancelCallbacksCopy;

        synchronized (lock)
        {
            running = false;
            cancelRequested = false;
            cancelCallbacksCopy = new ArrayList<>(cancelCallbacks);
            cancelCallbacks.clear();
        }

        for (Runnable callback : cancelCallbacksCopy)
        {
            callback.run();
        }
    }

    @Override
    public void start()
    {
        synchronized (lock)
        {
            running = true;
        }
    }

    @Override
    public void setProcessName(String processName)
    {
    }

    @Override
    public void setStageCount(int count)
    {
    }

    @Override
    public void setStage(int stage, String message)
    {
    }

    @Override
    public void advanceStage(String message)
    {
    }

    @Override
    public void setMaxProgress(double maxProgress)
    {
    }

    @Override
    public void setProgress(double progress, String message)
    {
    }

    @Override
    public void complete()
    {
        runCancelCallbacks();
    }

    @Override
    public void fail(Throwable e)
    {
        runCancelCallbacks();
    }

    @Override
    public boolean isConflictingProcess()
    {
        return false;
    }
}
