/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
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
 * Enumerates the six faces of a cubemap.
 */
public enum CubemapFace 
{
    /**
     * The face whose outward-facing normal is oriented in the +x direction.
     */
    POSITIVE_X,

    /**
     * The face whose outward-facing normal is oriented in the -x direction.
     */
    NEGATIVE_X,

    /**
     * The face whose outward-facing normal is oriented in the +y direction.
     */
    POSITIVE_Y,

    /**
     * The face whose outward-facing normal is oriented in the -y direction.
     */
    NEGATIVE_Y,

    /**
     * The face whose outward-facing normal is oriented in the +z direction.
     */
    POSITIVE_Z,

    /**
     * The face whose outward-facing normal is oriented in the -z direction.
     */
    NEGATIVE_Z
}
