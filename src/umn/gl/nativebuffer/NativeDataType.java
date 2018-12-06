/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.nativebuffer;

/**
 * An enumeration of the possible data types that can be represented in native memory buffers.
 */
public enum NativeDataType 
{
    UNSIGNED_BYTE(1),
    BYTE(1),
    PACKED_BYTE(1, true),
    UNSIGNED_SHORT(2),
    SHORT(2),
    PACKED_SHORT(2, true),
    UNSIGNED_INT(4),
    INT(4),
    PACKED_INT(4, true),
    FLOAT(4),
    DOUBLE(8);

    private final int sizeInBytes;
    private final boolean packed;

    NativeDataType(int sizeInBytes, boolean packed)
    {
        this.sizeInBytes = sizeInBytes;
        this.packed = packed;
    }

    NativeDataType(int sizeInBytes)
    {
        this(sizeInBytes, false);
    }

    public int getSizeInBytes()
    {
        return sizeInBytes;
    }

    public boolean isPacked()
    {
        return packed;
    }
}
