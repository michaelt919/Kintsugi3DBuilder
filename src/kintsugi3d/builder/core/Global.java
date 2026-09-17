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

package kintsugi3d.builder.core;

import kintsugi3d.builder.io.IOModel;

public final class Global
{
    private Global()
    {
    }

    /**
     * Gets state that is shared between threads
     * (typically owned by JavaFX for easy property binding, but accessible to other threads in a thread-safe manner)
     * @return
     */
    public static Kintsugi3DBuilderState state()
    {
        return GlobalBootstrap.getState();
    }

    public static IOModel io()
    {
        return GlobalBootstrap.getIOModel();
    }
}
