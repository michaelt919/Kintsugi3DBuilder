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

import kintsugi3d.gl.vecmath.IntVector2;

public interface ReadableFramebuffer<ContextType extends Context<ContextType>> extends Framebuffer<ContextType>
{
    /**
     * Gets a representation of the contents of this framebuffer fr reading.
     * @return A handle that can be used to perform operations that retrieve the contents of this framebuffer.
     */
    FramebufferReadContents<ContextType> getReadContents();


    /**
     * Gets an object that encapsulates read capabilities for this texture as a color texture.
     * @return the texture reader
     */
    ColorTextureReader getTextureReaderForColorAttachment(int attachmentIndex);

    /**
     * Gets an object that encapsulates read capabilities for this texture as a depth texture.
     * @return the texture reader
     */
    DepthTextureReader getTextureReaderForDepthAttachment();

    /**
     * Gets an object that encapsulates a viewport within this framebuffer that can be drawn to.
     * @param x The left edge of the viewport
     * @param y The bottom edge of the viewport
     * @param width The width of the viewport
     * @param height The height of the viewport
     * @return
     */
    @Override
    default ReadableFramebufferViewport<ContextType> getViewport(int x, int y, int width, int height)
    {
        return new ReadableFramebufferViewport<>(this, new IntVector2(x, y), new FramebufferSize(width, height));
    }
}
