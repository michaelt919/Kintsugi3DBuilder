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

import java.nio.ShortBuffer;

public interface DepthTextureReader
{
    /**
     * Gets the width of the texture.
     * @return The width of the texture.
     */
    int getWidth();

    /**
     * Gets the height of the texture.
     * @return The height of the texture.
     */
    int getHeight();

    /**
     * Reads the pixels from the texture as depth values.
     * Only a rectangular subset of the pixels will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void read(ShortBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as depth values.
     * The entire framebuffer will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    default void read(ShortBuffer destination)
    {
        this.read(destination, 0, 0, getWidth(), getHeight());
    }

    /**
     * Reads the pixels from the texture as depth values.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the depth values.
     */
    short[] read(int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as depth values.
     * The entire framebuffer will be read.
     * @return An array containing the depth values.
     */
    default short[] read()
    {
        return this.read(0, 0, getWidth(), getHeight());
    }
}
