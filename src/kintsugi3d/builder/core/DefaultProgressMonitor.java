/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
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
 * A default progress monitor implementation that does nothing for each method and never requests cancellation.
 */
public class DefaultProgressMonitor implements ProgressMonitor
{
    @Override
    public void allowUserCancellation() throws UserCancellationException
    {
    }

    @Override
    public void cancelComplete(UserCancellationException e)
    {
    }

    @Override
    public void start()
    {
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
    }

    @Override
    public void fail(Throwable e)
    {
    }

    @Override
    public boolean isConflictingProcess() {
        return false;
    }
}
