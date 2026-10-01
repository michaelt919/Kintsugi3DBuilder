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

package kintsugi3d.gl.interactive;

import kintsugi3d.gl.core.Context;

/**
 * An interface used with the InteractiveGraphics object to coordinate the initialization,
 * updating, drawing and deleting of an OpenGL-like renderable view.
 * 
 * @author Michael Tetzlaff
 * @see InteractiveGraphics
 */
public interface InteractiveRenderableResource<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{


    /**
     * Sets the application managing this renderable.
     * @param app
     */
    void setOwningApp(InteractiveApplication app);

    /**
     * Execute any initialization needed prior to updating and drawing this object.  You
     * should bring the internal state into being prepared to call update and draw.  Called
     * once by the associated InteractiveApplication created by InteractiveGraphics.  The
     * associated context will be made current first.
     */
    void initialize() throws InitializationException;

    /**
     * Execute any cleanup and bring the internal state out of being prepared to draw. Update
     * and draw will not execute after this method without initialize first being called. Called
     * once by the associated InteractiveApplication created by InteractiveGraphics when the
     * application is terminating.  The associated context will be made current first.
     */
    @Override
    void close();
}
