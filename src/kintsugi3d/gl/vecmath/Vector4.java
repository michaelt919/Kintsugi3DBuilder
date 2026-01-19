/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.vecmath;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.DoubleUnaryOperator;

/**
 * A vector of four dimensions (for linear algebra calculations) backed by 
 * 32-bit floats.  Useful for homogeneous coordinates in three dimensional
 * space.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 */
public class Vector4 implements Iterable<Float>
{
    /**
     * The first dimension
     */
    public final float x;
    /**
     * The second dimension
     */
    public final float y;
    /**
     * The third dimension
     */
    public final float z;
    /**
     * The fourth dimension (or heterogeneous coordinate)
     */
    public final float w;

    public static final Vector4 ZERO = Vector3.ZERO.asDirection();
    public static final Vector4 ORIGIN = Vector3.ZERO.asPosition();

    /**
     * Construct a vector in four dimensions with the given values.
     * @param x Value of the first dimension.
     * @param y Value of the second dimension.
     * @param z Value of the third dimension.
     * @param w Value of the fourth dimension (or heterogeneous coordinate).
     */
    public Vector4(float x, float y, float z, float w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4 (float value)
    {
        this(value, value, value, value);
    }

    public Vector2 getXY()
    {
        return new Vector2(this.x, this.y);
    }

    public Vector3 getXYZ()
    {
        return new Vector3(this.x, this.y, this.z);
    }

    public IntVector4 rounded()
    {
        return new IntVector4(Math.round(this.x), Math.round(this.y), Math.round(this.z), Math.round(this.w));
    }

    public IntVector4 truncated()
    {
        return new IntVector4((int)this.x, (int)this.y, (int)this.z, (int)this.w);
    }

    public DoubleVector4 asDoublePrecision()
    {
        return new DoubleVector4(x, y, z, w);
    }

    @Override
    @SuppressWarnings("FloatingPointEquality")
    public boolean equals(Object obj)
    {
        if (obj instanceof Vector4)
        {
            Vector4 other = (Vector4) obj;
            return other.x == this.x && other.y == this.y && other.z == this.z && other.w == this.w;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        if (this.equals(ZERO))
        {
            return 0;
        }
        else
        {
            int result = Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(z);
            return 31 * result + Float.floatToIntBits(w);
        }
    }

    /**
     * Construct a new vector as the sum of this one and the given parameter.
     * @param other The vector to add.
     * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
     */
    public Vector4 plus(Vector4 other)
    {
        return new Vector4(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z,
            this.w + other.w
        );
    }

    /**
     * Construct a new vector as the subtraction of the given parameter from this.
     * @param other The vector to add.
     * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
     */
    public Vector4 minus(Vector4 other)
    {
        return new Vector4(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z,
            this.w - other.w
        );
    }

    /**
     * Construct a new vector that is the negation of this.
     * @return A new vector with the values (-x, -y, -z, -w)
     */
    public Vector4 negated()
    {
        return new Vector4(-this.x, -this.y, -this.z, -this.w);
    }

    /**
     * Construct a new vector that is the product of this and a given scaler.
     * @param s The scaler to multiply by.
     * @return A new vector equal to (s*x, s*y, s*z, s*w)
     */
    public Vector4 times(float s)
    {
        return new Vector4(s*this.x, s*this.y, s*this.z, s*this.w);
    }

    /**
     * Construct a new vector that is the quotient of this and a given scaler.
     * @param s The scaler to divide by.
     * @return A new vector equal to (x/s, y/s, z/s, w/s)
     */
    public Vector4 dividedBy(float s)
    {
        return new Vector4(this.x/s, this.y/s, this.z/s, this.w/s);
    }

    public Vector4 times(Vector4 other)
    {
        return new Vector4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w);
    }

    public Vector4 dividedBy(Vector4 other)
    {
        return new Vector4(this.x / other.x, this.y / other.y, this.z / other.z, this.w * other.w);
    }
    /**
     * Compute the dot product (scaler product) of this vector and another given vector.
     * @param other The vector to use when computing the dot product.
     * @return A scaler value equal to the sum of x1*x2, y1*y2, z1*z2 and w1*w2.
     */
    public float dot(Vector4 other)
    {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    /**
     * Compute the outer product of this vector and another given vector.
     * @param other The vector to use when computing the outer product.
     * @return The matrix that is the outer product of the vectors.
     */
    public Matrix4 outerProduct(Vector4 other)
    {
        return Matrix4.fromColumns(
            new Vector4(this.x * other.x, this.y * other.x, this.z * other.x, this.w * other.x),
            new Vector4(this.x * other.y, this.y * other.y, this.z * other.y, this.w * other.y),
            new Vector4(this.x * other.z, this.y * other.z, this.z * other.z, this.w * other.z),
            new Vector4(this.x * other.w, this.y * other.w, this.z * other.w, this.w * other.w)
        );
    }

    /**
     * Compute a scaler value representing the length/magnitude of this vector.
     * @return A scaler value equal to square root of the sum of squares of the components.
     */
    public float length()
    {
        return (float)Math.sqrt(this.dot(this));
    }

    /**
     * Calculate the distance between this and another given vector.
     * @param other The vector to compute the distance between.
     * @return A scaler value equal to the length of the different vector.
     */
    public float distance(Vector4 other)
    {
        return this.minus(other).length();
    }

    /**
     * Create a new vector with the same direction as this one but with unit
     * magnitude (a length of 1.0).  CAUTION!  May cause divide by zero error.
     * Do not attempt to normalize a zero-length vector.
     * @return A new vector equal to this vector divided by it's length.
     */
    public Vector4 normalized()
    {
        return this.times(1.0f / this.length());
    }
    /**
     * Create a new vector with the same direction as this one but with unit
     * magnitude (a length of 1.0).  If this vector has zero length, it will return itself.
     * @return A new vector equal to this vector divided by it's length except in cases of a zero-length vector.
     */
    public Vector4 normalizedSafe()
    {
        float l = this.length();
        if (l == 0.0f)
        {
            return this;
        }
        else
        {
            return this.times(1.0f / l);
        }
    }

    public Vector4 applyOperator(DoubleUnaryOperator operator)
    {
        return new Vector4((float)operator.applyAsDouble(x), (float)operator.applyAsDouble(y), (float)operator.applyAsDouble(z), (float)operator.applyAsDouble(w));
    }

    /**
     * Applies operator to XYZ but leaves W unchanged.
     * @param operator
     * @return
     */
    public Vector4 applyOperatorXYZ(DoubleUnaryOperator operator)
    {
        return new Vector4((float)operator.applyAsDouble(x), (float)operator.applyAsDouble(y), (float)operator.applyAsDouble(z), w);
    }

    @Override
    public String toString()
    {
        return "Vector4{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", w=" + w +
            '}';
    }

    @Override
    public Iterator<Float> iterator()
    {
        return Arrays.asList(x, y, z, w).iterator();
    }
}
