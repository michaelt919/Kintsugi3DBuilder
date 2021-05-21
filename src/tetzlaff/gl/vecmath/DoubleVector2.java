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
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  This is an immutable object.
 * 
 * @see Vector2
 * @author Michael Tetzlaff
 */
public class DoubleVector2 implements Iterable<Double>
{
    public static final DoubleVector2 ZERO = new DoubleVector2(0.0f);

    /**
     * The first dimension
     */
    public final double x;

    /**
     * The second dimension
     */
    public final double y;

    /**
     * Construct a vector in two dimensions with the given values.
     * @param x Value of the first dimension.
     * @param y Value of the second dimension.
     */
    public DoubleVector2(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Construct a vector in two dimensions with the given values.
     * @param value Value of both dimensions.
     */
    public DoubleVector2(double value)
    {
        this(value, value);
    }

    public DoubleVector3 asVector3()
    {
        return asVector3(0.0f);
    }

    public DoubleVector3 asVector3(double z)
    {
        return new DoubleVector3(this.x, this.y, z);
    }

    public DoubleVector4 asPosition()
    {
        return asVector4(0.0f, 1.0f);
    }

    public DoubleVector4 asDirection()
    {
        return asVector4(0.0f, 0.0f);
    }

    public DoubleVector4 asVector4(double z, double w)
    {
        return new DoubleVector4(this.x, this.y, z, w);
    }

    public IntVector2 rounded()
    {
        return new IntVector2((int)Math.round(this.x), (int)Math.round(this.y));
    }

    public IntVector2 truncated()
    {
        return new IntVector2((int)this.x, (int)this.y);
    }

    public Vector2 asSinglePrecision()
    {
        return new Vector2((float)x, (float)y);
    }

    public DoubleVector2 plus(DoubleVector2 other)
    {
        return new DoubleVector2(
            this.x + other.x,
            this.y + other.y
        );
    }

    public DoubleVector2 minus(DoubleVector2 other)
    {
        return new DoubleVector2(
            this.x - other.x,
            this.y - other.y
        );
    }

    public DoubleVector2 negated()
    {
        return new DoubleVector2(-this.x, -this.y);
    }

    public DoubleVector2 times(double s)
    {
        return new DoubleVector2(s*this.x, s*this.y);
    }

    public DoubleVector2 dividedBy(double s)
    {
        return new DoubleVector2(this.x/s, this.y/s);
    }

    public double dot(DoubleVector2 other)
    {
        return this.x * other.x + this.y * other.y;
    }

    public double length()
    {
        return Math.sqrt(this.dot(this));
    }

    public double distance(DoubleVector2 other)
    {
        return this.minus(other).length();
    }

    public DoubleVector2 normalized()
    {
        return this.times(1.0 / this.length());
    }

    public DoubleVector2 applyOperator(DoubleUnaryOperator operator)
    {
        return new DoubleVector2(operator.applyAsDouble(x), operator.applyAsDouble(y));
    }

    @Override
    public String toString()
    {
        return "DoubleVector2{" +
            "x=" + x +
            ", y=" + y +
            '}';
    }

    @Override
    public Iterator<Double> iterator()
    {
        return Arrays.asList(x, y).iterator();
    }
}
