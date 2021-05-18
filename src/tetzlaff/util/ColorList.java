/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.util;

import java.util.AbstractList;

import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.gl.vecmath.Vector4;

public class ColorList extends AbstractList<Vector4>
{
    private final float[] colorData;

    public ColorList(float[] colorData)
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.colorData = colorData;
    }

    @Override public Vector4 get(int p)
    {
        return new Vector4(colorData[4 * p], colorData[4 * p + 1], colorData[4 * p + 2], colorData[4 * p + 3]);
    }

    @Override public int size() { return colorData.length / 4; }
}
