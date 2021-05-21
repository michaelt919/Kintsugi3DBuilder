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

package tetzlaff.gl.vecmath;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A vector of four dimensions (for linear algebra calculations) backed by 
 * 32-bit integers.
 * All arithmetic other than length() or distance() will be integer arithmetic, including division.  
 * This is an immutable object.
 * 
 * @see Vector4
 * @author Michael Tetzlaff
 */
public class IntVector4 implements Iterable<Integer>
{
    public final int x;
    public final int y;
    public final int z;
    public final int w;

    public IntVector4(int x, int y, int z, int w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public IntVector4(int value)
    {
        this(value, value, value, value);
    }

    public Vector4 asFloatingPoint()
    {
        return new Vector4(x, y, z, w);
    }

    public Vector4 asFloatingPointNormalized()
    {
        return new Vector4(x / 255.0f, y / 255.0f, z / 255.0f, w / 255.0f);
    }

    public IntVector2 getXY()
    {
        return new IntVector2(this.x, this.y);
    }

    public IntVector3 getXYZ()
    {
        return new IntVector3(this.x, this.y, this.z);
    }

    public IntVector4 plus(IntVector4 other)
    {
        return new IntVector4(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z,
            this.w + other.w
        );
    }

    public IntVector4 minus(IntVector4 other)
    {
        return new IntVector4(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z,
            this.w - other.w
        );
    }

    public IntVector4 negated()
    {
        return new IntVector4(-this.x, -this.y, -this.z, -this.w);
    }

    public IntVector4 times(int s)
    {
        return new IntVector4(s*this.x, s*this.y, s*this.z, s*this.w);
    }

    public IntVector4 dividedBy(int s)
    {
        return new IntVector4(this.x/s, this.y/s, this.z/s, this.w/s);
    }

    public int dot(IntVector4 other)
    {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    public double length()
    {
        return Math.sqrt(this.dot(this));
    }

    public double distance(IntVector4 other)
    {
        return this.minus(other).length();
    }

    @Override
    public Iterator<Integer> iterator()
    {
        return Arrays.asList(x, y, z, w).iterator();
    }
}
