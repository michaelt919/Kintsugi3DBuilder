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
 * Enumerates the possible wrap modes for a texture in a particular direction.
 */
public enum TextureWrapMode 
{
    /**
     * Do not wrap at all; clamp texture coordinate to the range [0, 1].
     */
    None,

    /**
     * Mirror the image once and then clamp texture coordinate to the range [0, 2].
     */
    MirrorOnce,

    /**
     * Repeat the texture as the texture coordinate increases.
     */
    Repeat,

    /**
     * Repeat the texture but alternate the orientation each time as the texture coordinate increases.
     */
    MirroredRepeat
}
