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

package kintsugi3d.builder.core;

import kintsugi3d.gl.core.TwoDimensional;

public class TextureResolution
{
    public final int width;
    public final int height;

    public TextureResolution(int width, int height)
    {
        if (width <= 0)
        {
            throw new IllegalArgumentException("Texture width must be greater than zero.");
        }
        else if (height <= 0)
        {
            throw new IllegalArgumentException("Texture height must be greater than zero.");
        }

        this.width = width;
        this.height = height;
    }

    /**
     * Bundles width and height of a texture
     * @param texture
     * @return
     */
    public static TextureResolution of(TwoDimensional texture)
    {
        return new TextureResolution(texture.getWidth(), texture.getHeight());
    }
}
