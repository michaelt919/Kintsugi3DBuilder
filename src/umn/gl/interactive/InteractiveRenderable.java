/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.interactive;

import umn.gl.core.Context;
import umn.gl.core.Framebuffer;
import umn.interactive.InitializationException;

/**
 * An interface used with the InteractiveGraphics object to coordinate the initialization,
 * updating, drawing and deleting of an OpenGL-like renderable view.
 * 
 * @author Michael Tetzlaff
 * @see InteractiveGraphics
 */
public interface InteractiveRenderable<ContextType extends Context<ContextType>> extends AutoCloseable
{
    /**
     * Execute any initialization needed prior to updating and drawing this object.  You
     * should bring the internal state into being prepared to call update and draw.  Called
     * once by the associated InteractiveApplication created by InteractiveGraphics.  The
     * associated context will be made current first.
     */
    void initialize() throws InitializationException;

    /**
     * Adjust internal state that needs to change prior to drawing.  Called every time the
     * associated InteractiveApplication object refreshes and before draw() is called.
     * The associated context will be made current first.
     * This method may also be called without subsequently calling draw() to allow its internal state to be updated only.
     */
    void update();

    /**
     * Interpret the internal state and draw this object.  Called every time the associated
     * InteractiveApplication object refreshes and immediately after update is called.
     * The associated context will be made current first.  Generally, the object should be
     * immutable (no internal state should change) while executing this method.
     */
    void draw(Framebuffer<ContextType> framebuffer);

    /**
     * Execute any cleanup and bring the internal state out of being prepared to draw. Update
     * and draw will not execute after this method without initialize first being called. Called
     * once by the associated InteractiveApplication created by InteractiveGraphics when the
     * application is terminating.  The associated context will be made current first.
     */
    @Override
    void close();
}
