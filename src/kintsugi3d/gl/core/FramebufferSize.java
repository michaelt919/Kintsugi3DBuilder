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
 * A simple structure for representing the dimensions of a framebuffer.
 * @author Michael Tetzlaff
 *
 */
public class FramebufferSize
{
    /**
     * The width of the framebuffer.
     */
    public final int width;

    /**
     * The height of the framebuffer.
     */
    public final int height;

    /**
     * Creates a new FramebufferSize structure.
     * @param width The width of the framebuffer.
     * @param height The height of the framebuffer.
     */
    public FramebufferSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof FramebufferSize)
        {
            FramebufferSize other = (FramebufferSize)obj;
            return other.width == this.width && other.height == this.height;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int result = width;
        result = 31 * result + height;
        return result;
    }
}
