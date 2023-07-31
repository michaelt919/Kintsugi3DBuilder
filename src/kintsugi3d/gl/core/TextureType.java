/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.core;

/**
 * Enumerates the possible texture types.
 */
public enum TextureType 
{
    /**
     * A "null" texture for representing the absence of a texture in a particular context.
     */
    NULL,

    /**
     * A traditional color texture map.
     */
    COLOR,

    /**
     * A depth or "z" buffer such as for hidden surface removal, shadows, etc.
     */
    DEPTH,

    /**
     * A depth buffer where the depth values are stored as floating-point numbers.
     */
    FLOATING_POINT_DEPTH,

    /**
     * A stencil buffer.
     */
    STENCIL,

    /**
     * A combined depth / stencil buffer.
     */
    DEPTH_STENCIL,

    /**
     * A combined depth / stencil buffer where the depth values are stored as floating-point numbers.
     */
    FLOATING_POINT_DEPTH_STENCIL
}
