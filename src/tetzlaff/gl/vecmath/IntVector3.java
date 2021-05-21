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

package tetzlaff.gl.vecmath;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A vector of three dimensions (for linear algebra calculations) backed by 
 * 32-bit integers.
 * All arithmetic other than length() or distance() will be integer arithmetic, including division.  
 * This is an immutable object.
 * 
 * @author Michael Tetzlaff
 * @see Vector3
 */
public class IntVector3 implements Iterable<Integer>
{
    public final int x;
    public final int y;
    public final int z;

    public IntVector3(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Construct a vector in three dimensions with the given values.
     * @param value Value of all three dimensions.
     */
    public IntVector3(int value)
    {
        this(value, value, value);
    }

    public IntVector4 asVector4(int w)
    {
        return new IntVector4(this.x, this.y, this.z, w);
    }

    public Vector3 asFloatingPoint()
    {
        return new Vector3(x, y, z);
    }

    public Vector3 asFloatingPointNormalized()
    {
        return new Vector3(x / 255.0f, y / 255.0f, z / 255.0f);
    }

    public IntVector2 getXY()
    {
        return new IntVector2(this.x, this.y);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof IntVector3)
        {
            IntVector3 other = (IntVector3) obj;
            return other.x == this.x && other.y == this.y && other.z == this.z;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * (31 + x) + y) + z;
    }

    public IntVector3 plus(IntVector3 other)
    {
        return new IntVector3(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        );
    }

    public IntVector3 minus(IntVector3 other)
    {
        return new IntVector3(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z
        );
    }

    public IntVector3 negated()
    {
        return new IntVector3(-this.x, -this.y, -this.z);
    }

    public IntVector3 times(int s)
    {
        return new IntVector3(s*this.x, s*this.y, s*this.z);
    }

    public IntVector3 dividedBy(int s)
    {
        return new IntVector3(this.x/s, this.y/s, this.z/s);
    }

    public int dot(IntVector3 other)
    {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public IntVector3 cross(IntVector3 other)
    {
        return new IntVector3(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }

    public double length()
    {
        return Math.sqrt(this.dot(this));
    }

    public double distance(IntVector3 other)
    {
        return this.minus(other).length();
    }

    @Override
    public String toString()
    {
        return "IntVector3{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }

    @Override
    public Iterator<Integer> iterator()
    {
        return Arrays.asList(x, y, z).iterator();
    }
}
