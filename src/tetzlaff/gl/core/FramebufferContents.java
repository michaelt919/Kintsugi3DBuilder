/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.core;

/**
 * Encapsulates the logical concept of the "contents" of a framebuffer on the GPU.  These methods will typically not be
 * called directly, but can be invoked by Drawable instances or the framebuffer itself to prepare for read or draw commands.
 * @param <ContextType>
 */
public interface FramebufferContents<ContextType extends Context<ContextType>> extends Contextual<ContextType>
{
    /**
     * Gets the size of the framebuffer for which these are the contents.
     * @return
     */
    FramebufferSize getSize();

    /**
     * Prepares the contents of a framebuffer to be used for drawing within the specified viewport.
     * From the invokation of this function until another framebuffer's contents are bound, this framebuffer should
     * be prepared to accept any drawing commands using the specified viewport.
     * @param x The x-coordinate of the left edge of the viewport
     * @param y The y-coordinate of the bottom edge of the viewport
     * @param width The width of the viewport
     * @param height THe height of the viewport
     */
    void bindForDraw(int x, int y, int width, int height);

    /**
     * Prepares the contents of a framebuffer to be used for drawing within the entire framebuffer.
     * From the invokation of this function until another framebuffer's contents are bound, this framebuffer should
     * be prepared to accept any drawing commands.
     */
    default void bindForDraw()
    {
        FramebufferSize size = getSize();
        bindForDraw(0, 0, size.width, size.height);
    }

    /**
     * Prepares the contents of a particular framebuffer attachment to be used for read operations
     * From the invokation of this function until another framebuffer attachment is bound, this framebuffer should
     * be prepared to accept any read requests.
     * @param attachmentIndex The attachment to be used for reading.
     */
    void bindForRead(int attachmentIndex);
}
