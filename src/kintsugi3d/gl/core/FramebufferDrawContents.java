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

package kintsugi3d.gl.core;

/**
 * Encapsulates the logical concept of the "contents" of a framebuffer for drawing on the GPU.  These methods will typically not be
 * called directly, but can be invoked by Drawable instances or the framebuffer itself to prepare for draw commands.
 * @param <ContextType>
 */
public interface FramebufferDrawContents<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
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
    void bindViewportForDraw(int x, int y, int width, int height);

    /**
     * Prepares the contents of a framebuffer to be used for drawing within the entire framebuffer.
     * From the invokation of this function until another framebuffer's contents are bound, this framebuffer should
     * be prepared to accept any drawing commands.
     */
    default void bindForDraw()
    {
        FramebufferSize size = getSize();
        bindViewportForDraw(0, 0, size.width, size.height);
    }

    /**
     * Prepares the contents of a particular framebuffer attachment to be used for draw operations.
     * This is primarily intended for blit operations.
     * From the invokation of this function until another framebuffer attachment is bound, this framebuffer should
     * be prepared to accept any draw requests.
     * @param attachmentIndex The attachment to be used for reading.
     */
    void bindSingleAttachmentForDraw(int attachmentIndex);

    /**
     * Prepares the contents of a non-color (i.e. depth, stencil) attachment to be used for draw operations.
     * This is primarily intended for blit operations.
     * From the invokation of this function until another framebuffer attachment is bound, this framebuffer should
     * be prepared to accept any draw requests.
     */
    void bindNonColorAttachmentsForDraw();
}
