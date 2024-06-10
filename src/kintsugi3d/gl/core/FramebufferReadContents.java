/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
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
 * Encapsulates the logical concept of the "contents" of a framebuffer for reading on the GPU.  These methods will typically not be
 * called directly, but can be invoked by Drawable instances or the framebuffer itself to prepare for read commands.
 * @param <ContextType>
 */
public interface FramebufferReadContents<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
    /**
     * Gets the size of the framebuffer for which these are the contents.
     * @return
     */
    FramebufferSize getSize();

    /**
     * Prepares the contents of a particular framebuffer attachment to be used for read operations
     * From the invokation of this function until another framebuffer attachment is bound, this framebuffer should
     * be prepared to accept any read requests.
     * @param attachmentIndex The attachment to be used for reading.
     */
    void bindForRead(int attachmentIndex);

    /**
     * Prepares the contents of a non-color (i.e. depth, stencil) attachment to be used for read operations.
     * This is primarily intended for blit operations.
     * From the invokation of this function until another framebuffer attachment is bound, this framebuffer should
     * be prepared to accept any read requests.
     */
    void bindNonColorAttachmentForRead();
}
