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

package kintsugi3d.gl.core;

import java.util.function.Consumer;

/**
 * An interface for a double-buffered framebuffer that internally contains two offscreen FBOs.
 * @param <ContextType>
 */
public interface DoubleFramebufferObject<ContextType extends Context<ContextType>>
    extends DoubleFramebuffer<ContextType>, ReadableFramebuffer<ContextType>, Swappable,
            SwapObservable<ReadableFramebuffer<ContextType>>, ResizeObservable<Framebuffer<ContextType>>, ManagedResource
{
    /**
     * Request that the FBOs be resized.
     * @param width The new width for the FBO.
     * @param height The new height for the FBO.
     */
    void requestResize(int width, int height);

    /**
     * Add a listener that will be called whenever the framebuffers swap,
     * consuming a reference to the new front framebuffer.
     * @param listener The listener that runs when a framebuffer swap occurs with a reference to the front framebuffer.
     */
    @Override
    void addSwapListener(Consumer<ReadableFramebuffer<ContextType>> listener);

    /**
     * Add a listener that will be called whenever the framebuffer is resized,
     * consuming a reference to the back framebuffer that was just resized.
     *
     * @param listener The listener that runs when a framebuffer is resized with a reference to the resized back framebuffer.
     */
    @Override
    void addResizeListener(Consumer<Framebuffer<ContextType>> listener);
}
