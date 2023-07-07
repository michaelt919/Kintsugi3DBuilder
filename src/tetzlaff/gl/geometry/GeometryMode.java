/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.geometry;

import tetzlaff.gl.core.IntLike;

public enum GeometryMode implements IntLike
{
    PROJECT_3D_TO_2D(0),
    RECTANGLE(1);

    private final int intValue;

    GeometryMode(int intValue)
    {
        this.intValue = intValue;
    }

    /**
     * For uniforms and vertex attributes
     * @return the integer encoding of this instance
     */
    @Override
    public int getIntValue()
    {
        return intValue;
    }

    /**
     * For preprocessor defines
     * @return the String form of the integer encoding of this instance
     */
    public String toString()
    {
        return Integer.toString(intValue);
    }
}
