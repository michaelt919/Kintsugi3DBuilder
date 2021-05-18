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
 * @author Michael Tetzlaff
 * 
 * A vector of four dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  Useful for heterogeneous coordinate calculations. This
 * is an immutable object.
 * 
 * @see Vector4
 */
public class DoubleVector4 implements Iterable<Double>
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

    /**
     * The fourth dimension (or heterogeneous coordinate)
     */
    public final double w;

    public static final DoubleVector4 ZERO = DoubleVector3.ZERO.asDirection();
    public static final DoubleVector4 ORIGIN = DoubleVector3.ZERO.asPosition();

    /**
     * Construct a vector in four dimensions with the given values.
     * @param x Value of the first dimension.
     * @param y Value of the second dimension.
     * @param z Value of the third dimension.
     * @param w Value of the fourth dimension (or heterogeneous coordinate).
     */
    public DoubleVector4(double x, double y, double z, double w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public DoubleVector4 (double value)
    {
        this(value, value, value, value);
    }

    public DoubleVector2 getXY()
    {
        return new DoubleVector2(this.x, this.y);
    }

    public DoubleVector3 getXYZ()
    {
        return new DoubleVector3(this.x, this.y, this.z);
    }

    public IntVector4 rounded()
    {
        return new IntVector4((int)Math.round(this.x), (int)Math.round(this.y), (int)Math.round(this.z), (int)Math.round(this.w));
    }

    public IntVector4 truncated()
    {
        return new IntVector4((int)this.x, (int)this.y, (int)this.z, (int)this.w);
    }

    public Vector4 asSinglePrecision()
    {
        return new Vector4((float)x, (float)y, (float)z, (float)w);
    }

    public DoubleVector4 plus(DoubleVector4 other)
    {
        return new DoubleVector4(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z,
            this.w + other.w
        );
    }

    public DoubleVector4 minus(DoubleVector4 other)
    {
        return new DoubleVector4(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z,
            this.w - other.w
        );
    }

    public DoubleVector4 negated()
    {
        return new DoubleVector4(-this.x, -this.y, -this.z, -this.w);
    }

    public DoubleVector4 times(double s)
    {
        return new DoubleVector4(s*this.x, s*this.y, s*this.z, s*this.w);
    }

    public DoubleVector4 dividedBy(double s)
    {
        return new DoubleVector4(this.x/s, this.y/s, this.z/s, this.w/s);
    }

    public double dot(DoubleVector4 other)
    {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    /**
     * Compute the outer product of this vector and another given vector.
     * @param other The vector to use when computing the outer product.
     * @return The matrix that is the outer product of the vectors.
     */
    public DoubleMatrix4 outerProduct(DoubleVector4 other)
    {
        return DoubleMatrix4.fromColumns(
            new DoubleVector4(this.x * other.x, this.y * other.x, this.z * other.x, this.w * other.x),
            new DoubleVector4(this.x * other.y, this.y * other.y, this.z * other.y, this.w * other.y),
            new DoubleVector4(this.x * other.z, this.y * other.z, this.z * other.z, this.w * other.z),
            new DoubleVector4(this.x * other.w, this.y * other.w, this.z * other.w, this.w * other.w)
        );
    }

    public double length()
    {
        return Math.sqrt(this.dot(this));
    }

    public double distance(DoubleVector4 other)
    {
        return this.minus(other).length();
    }

    public DoubleVector4 normalized()
    {
        return this.times(1.0 / this.length());
    }

    public DoubleVector4 applyOperator(DoubleUnaryOperator operator)
    {
        return new DoubleVector4(operator.applyAsDouble(x), operator.applyAsDouble(y), operator.applyAsDouble(z), operator.applyAsDouble(w));
    }

    @Override
    public String toString()
    {
        return "DoubleVector4{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", w=" + w +
            '}';
    }

    @Override
    public Iterator<Double> iterator()
    {
        return Arrays.asList(x, y, z, w).iterator();
    }
}
