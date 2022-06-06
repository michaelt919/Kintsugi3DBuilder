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

package tetzlaff.gl.core;

import java.util.function.Consumer;

/**
 * An interface for a double-buffered framebuffer that internally contains two offscreen FBOs.
 * @param <ContextType>
 */
public interface DoubleFramebufferObject<ContextType extends Context<ContextType>>
    extends DoubleFramebuffer<ContextType>, Resource
{
    /**
     * Request that the FBOs be resized.
     * @param width The new width for the FBO.
     * @param height The new height for the FBO.
     */
    void requestResize(int width, int height);

    /**
     * Add a listener that will be called whenever the framebuffers swap.
     * @param listener The listener that runs when a framebuffer swap occurs.
     */
    void addSwapListener(Consumer<Framebuffer<ContextType>> listener);
}
