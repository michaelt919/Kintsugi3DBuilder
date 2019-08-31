/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

public enum BufferAccessType 
{
    /**
     * The buffer will be primarily written to by the application and read by shaders.
     */
    DRAW,

    /**
     * The buffer will be primarily written to by shaders and read by the application.
     */
    READ,

    /**
     * The buffer will be primarily both written to and read from shaders.
     */
    COPY
}
