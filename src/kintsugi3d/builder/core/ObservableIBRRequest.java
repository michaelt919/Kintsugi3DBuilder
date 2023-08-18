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

import kintsugi3d.gl.core.Context;

/**
 * An interface for an executable that requires a loaded model instance and can be observed by a loading monitor
 */
public interface ObservableIBRRequest extends IBRRequest
{
    /**
     * The entry point for the executable.
     * @param renderable The implementation of Kintsugi 3D Builder's renderer.
     *                   This can be used to dynamically generate renders of the current view,
     *                   or just to access the IBRResources and the graphics Context.
     * @param callback A callback that can be fired to update the loading bar.
     *                 If this is unused, an "infinite loading" indicator will be displayed instead.
     * @param <ContextType> The type of the graphics context that the renderer implementation uses.
     * @throws Exception An exception may be thrown by the executable that will be caught and logged by Kintsugi 3D Builder.
     */
    <ContextType extends Context<ContextType>> void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws Exception;

    default <ContextType extends Context<ContextType>> void executeRequest(IBRInstance<ContextType> renderable) throws Exception
    {
        // Use a default LoadingMonitor that does nothing but doesn't cause null pointer exceptions
        this.executeRequest(renderable, new LoadingMonitor()
        {
            @Override
            public void startLoading()
            {
            }

            @Override
            public void setMaximum(double maximum)
            {
            }

            @Override
            public void setProgress(double progress)
            {
            }

            @Override
            public void loadingComplete()
            {
            }

            @Override
            public void loadingFailed(Exception e)
            {
            }
        });
    }
}
