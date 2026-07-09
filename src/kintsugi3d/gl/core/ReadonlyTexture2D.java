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

public interface ReadonlyTexture2D<ContextType extends Context<ContextType>>
    extends Texture<ContextType>, Croppable<Texture2D<ContextType>>, TwoDimensional
{
    /**
     * Gets the width of the texture.
     *
     * @return The width of the texture.
     */
    @Override
    int getWidth();

    /**
     * Gets the height of the texture.
     *
     * @return The height of the texture.
     */
    @Override
    int getHeight();

    /**
     * Creates a new, empty texture with different dimensions but the same internal format and settings as this texture.
     * Especially intended to be used with framebuffer blitting to ensure compatibility.
     *
     * @param newWidth  The width of the new texture.
     * @param newHeight The height of the new texture
     * @return The newly created texture.
     */
    Texture2D<ContextType> createTextureWithMatchingFormat(int newWidth, int newHeight);

    /**
     * Creates a new texture that contains a cropped region of this texture.
     * The texture this method is called on will remain unchanged.
     * @param x The left boundary of the cropped region
     * @param y The bottom boundary of the cropped region
     * @param cropWidth The width of the cropped region
     * @param cropHeight The height of the cropped region
     * @return The new cropped texture.
     */
    @Override
    Texture2D<ContextType> crop(int x, int y, int cropWidth, int cropHeight);

    /**
     * Copies this texture, creating a new resource with identical contents.
     * @return The new resource containing a copy of the texture
     */
    default ReadonlyTexture2D<ContextType> copy()
    {
        return this.crop(0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Gets an object that encapsulates read capabilities for this texture as a color texture.
     * @return the texture reader
     */
    ColorTextureReader getColorTextureReader();

    /**
     * Gets an object that encapsulates read capabilities for this texture as a depth texture.
     * @return the texture reader
     */
    DepthTextureReader getDepthTextureReader();
}
