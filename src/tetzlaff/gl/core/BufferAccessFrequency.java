/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

/**
 * Enumerates the possibly modes of frequency with which a buffer may be accessed.
 * @author Michael Tetzlaff
 *
 */
public enum BufferAccessFrequency 
{
    /**
     * The buffer will be written to once, and only read from a few times.
     */
    STREAM,

    /**
     * The buffer will be written to once, and read from many times.
     */
    STATIC,

    /**
     * The buffer may be written to or read from many times.
     */
    DYNAMIC
}
