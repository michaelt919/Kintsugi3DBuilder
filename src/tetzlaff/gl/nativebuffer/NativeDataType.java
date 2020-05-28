/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.nativebuffer;

public enum NativeDataType 
{
    UNSIGNED_BYTE(1),
    BYTE(1),
    UNSIGNED_SHORT(2),
    SHORT(2),
    UNSIGNED_INT(4),
    INT(4),
    FLOAT(4),
    DOUBLE(8);

    private final int sizeInBytes;

    NativeDataType(int sizeInBytes)
    {
        this.sizeInBytes = sizeInBytes;
    }

    public int getSizeInBytes()
    {
        return sizeInBytes;
    }
}
