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

package kintsugi3d.gl.interactive;

import kintsugi3d.builder.core.DefaultProgressMonitor;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.gl.core.Context;

/**
 * An interface for an executable that only requires a graphics context (no pre-loaded data)
 * and can be observed by a loading monitor
 */
public interface ObservableGraphicsRequest extends GraphicsRequest
{
    /**
     * The entry point for the executable.
     * @param context The graphics context to be used.
     * @param monitor A monitor that can be fired to update the loading bar.
     *                 If this is unused, an "infinite loading" indicator will be displayed instead.
     * @param <ContextType> The type of the graphics context that the renderer implementation uses.
     * @throws Exception An exception may be thrown by the executable that will be caught and logged by Kintsugi 3D Builder.
     */
    <ContextType extends Context<ContextType>> void executeRequest(ContextType context, ProgressMonitor monitor) throws Exception;

    @Override
    default <ContextType extends Context<ContextType>> void executeRequest(ContextType context) throws Exception
    {
        // Use a default ProgressMonitor that does nothing but doesn't cause null pointer exceptions
        this.executeRequest(context, new DefaultProgressMonitor());
    }
}