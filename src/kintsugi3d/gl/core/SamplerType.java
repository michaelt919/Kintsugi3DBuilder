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
 * Enumerates the types of samplers that can be used with textures.
 */
public enum SamplerType
{
    /**
     * A 1D texture with floating-point pixel values.
     */
    FLOAT_1D,

    /**
     * A 2D texture with floating-point pixel values.
     */
    FLOAT_2D,

    /**
     * A 3D texture with floating-point pixel values.
     */
    FLOAT_3D,

    /**
     * A cubemap with floating-point pixel values.
     */
    FLOAT_CUBE_MAP,

    /**
     * An array of 1D textures with floating-point pixel values.
     */
    FLOAT_1D_ARRAY,

    /**
     * An array of 2D textures with floating-point pixel values.
     */
    FLOAT_2D_ARRAY,

    /**
     * A 1D texture with integer pixel values.
     */
    INTEGER_1D,

    /**
     * A 2D texture with integer pixel values.
     */
    INTEGER_2D,

    /**
     * A 3D texture with integer pixel values.
     */
    INTEGER_3D,

    /**
     * A cubemap with integer pixel values.
     */
    INTEGER_CUBE_MAP,

    /**
     * An array of 1D texture with integer pixel values.
     */
    INTEGER_1D_ARRAY,

    /**
     * An array of 2D texture with integer pixel values.
     */
    INTEGER_2D_ARRAY,

    /**
     * A 1D texture with unsigned integer pixel values.
     */
    UNSIGNED_INTEGER_1D,

    /**
     * A 2D texture with unsigned integer pixel values.
     */
    UNSIGNED_INTEGER_2D,

    /**
     * A 3D texture with unsigned integer pixel values.
     */
    UNSIGNED_INTEGER_3D,

    /**
     * A cubemap with unsigned integer pixel values.
     */
    UNSIGNED_INTEGER_CUBE_MAP,

    /**
     * An array of 1D textures with unsigned integer pixel values.
     */
    UNSIGNED_INTEGER_1D_ARRAY,

    /**
     * An array of 2D textures with unsigned integer pixel values.
     */
    UNSIGNED_INTEGER_2D_ARRAY
}
