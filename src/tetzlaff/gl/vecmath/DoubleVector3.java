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
import java.util.function.DoubleUnaryOperator;

/**
 * A vector of three dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 * @see Vector3
 */
public class DoubleVector3 implements Iterable<Double>
{
    /**
     * The first dimension
     */
    public final double x;

    /**
     * The second dimension
     */
    public final double y;

    /**
     * The third dimension
     */
    public final double z;

    public static final DoubleVector3 ZERO = new DoubleVector3(0.0);

    /**
     * Construct a vector in three dimensions with the given values.
     * @param x Value of the first dimension.
     * @param y Value of the second dimension.
     * @param z Value of the third dimension.
     */
    public DoubleVector3(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Construct a vector in three dimensions with the given values.
     * @param value Value of all three dimensions.
     */
    public DoubleVector3(double value)
    {
        this(value, value, value);
    }

    public DoubleVector4 asVector4(double w)
    {
        return new DoubleVector4(this.x, this.y, this.z, w);
    }

    public DoubleVector4 asDirection()
    {
        return new DoubleVector4(this.x, this.y, this.z, 0.0);
    }

    public DoubleVector4 asPosition()
    {
        return new DoubleVector4(this.x, this.y, this.z, 1.0);
    }

    public DoubleVector2 getXY()
    {
        return new DoubleVector2(this.x, this.y);
    }

    public IntVector3 rounded()
    {
        return new IntVector3((int)Math.round(this.x), (int)Math.round(this.y), (int)Math.round(this.z));
    }

    public IntVector3 truncated()
    {
        return new IntVector3((int)this.x, (int)this.y, (int)this.z);
    }


    public DoubleVector3 plus(DoubleVector3 other)
    {
        return new DoubleVector3(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        );
    }

    public DoubleVector3 minus(DoubleVector3 other)
    {
        return new DoubleVector3(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z
        );
    }

    public DoubleVector3 negated()
    {
        return new DoubleVector3(-this.x, -this.y, -this.z);
    }

    public DoubleVector3 times(double s)
    {
        return new DoubleVector3(s*this.x, s*this.y, s*this.z);
    }

    public DoubleVector3 dividedBy(double s)
    {
        return new DoubleVector3(this.x/s, this.y/s, this.z/s);
    }

    public double dot(DoubleVector3 other)
    {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public DoubleVector3 cross(DoubleVector3 other)
    {
        return new DoubleVector3(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }
    public DoubleMatrix3 outerProduct(DoubleVector3 other)
    {
        return DoubleMatrix3.fromColumns(
            new DoubleVector3(this.x * other.x, this.y * other.x, this.z * other.x),
            new DoubleVector3(this.x * other.y, this.y * other.y, this.z * other.y),
            new DoubleVector3(this.x * other.z, this.y * other.z, this.z * other.z)
        );
    }

    public double length()
    {
        return Math.sqrt(this.dot(this));
    }

    public double distance(DoubleVector3 other)
    {
        return this.minus(other).length();
    }

    public DoubleVector3 normalized()
    {
        return this.times(1.0 / this.length());
    }

    public DoubleVector3 applyOperator(DoubleUnaryOperator operator)
    {
        return new DoubleVector3(operator.applyAsDouble(x), operator.applyAsDouble(y), operator.applyAsDouble(z));
    }

    @Override
    public String toString()
    {
        return "DoubleVector3{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }

    @Override
    public Iterator<Double> iterator()
    {
        return Arrays.asList(x, y, z).iterator();
    }
}
