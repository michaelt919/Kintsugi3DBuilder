/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;

import kintsugi3d.gl.core.Context;

/**
 * An interface for an executable that can be launched from the IBRelight "Export" menu.
 * @param <ContextType> The type of the graphics context that the renderer implementation uses.
 */
public interface IBRRequest<ContextType extends Context<ContextType>>
{
    /**
     * The entry point for the executable.
     * @param renderable The implementation of the IBRelight renderer.
     *                   This can be used to dynamically generate renders of the current view,
     *                   or just to access the IBRResources and the graphics Context.
     * @param callback A callback that can be fired to update the loading bar.
     *                 If this is unused, an "infinite loading" indicator will be displayed instead.
     * @throws Exception An exception may be thrown by the executable that will be caught and logged by IBRelight.
     */
     void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws Exception;
}
