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
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.gl.core.ManagedResource;

public interface InteractiveRenderable<ContextType extends Context<ContextType>> extends ManagedResource
{
    /**
     * Gets the application managing this renderable.
     *
     * @return
     */
    InteractiveApplication getOwningApp();

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
}
