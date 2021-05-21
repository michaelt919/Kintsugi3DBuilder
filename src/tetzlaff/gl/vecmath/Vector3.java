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
import java.util.function.DoubleUnaryOperator;

/**
 * A vector of three dimensions (for linear algebra calculations) backed by 
 * 32-bit floats.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 */
public class Vector3 implements Iterable<Float>
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

    public static final Vector3 ZERO = new Vector3(0.0f);

    /**
     * Construct a vector in three dimensions with the given values.
     * @param x Value of the first dimension.
     * @param y Value of the second dimension.
     * @param z Value of the third dimension.
     */
    public Vector3(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Construct a vector in three dimensions with the given values.
     * @param value Value of all three dimensions.
     */
    public Vector3(float value)
    {
        this(value, value, value);
    }

    public Vector4 asVector4(float w)
    {
        return new Vector4(this.x, this.y, this.z, w);
    }

    public Vector4 asDirection()
    {
        return new Vector4(this.x, this.y, this.z, 0.0f);
    }

    public Vector4 asPosition()
    {
        return new Vector4(this.x, this.y, this.z, 1.0f);
    }

    public Vector2 getXY()
    {
        return new Vector2(this.x, this.y);
    }

    public IntVector3 rounded()
    {
        return new IntVector3(Math.round(this.x), Math.round(this.y), Math.round(this.z));
    }

    public IntVector3 truncated()
    {
        return new IntVector3((int)this.x, (int)this.y, (int)this.z);
    }

    @Override
    @SuppressWarnings("FloatingPointEquality")
    public boolean equals(Object obj)
    {
        if (obj instanceof Vector3)
        {
            Vector3 other = (Vector3) obj;
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
        if (this.equals(ZERO))
        {
            return 0;
        }
        else
        {
            int result = Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            return 31 * result + Float.floatToIntBits(z);
        }
    }

    /**
     * Construct a new vector as the sum of this one and the given parameter.
     * @param other The vector to add.
     * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
     */
    public Vector3 plus(Vector3 other)
    {
        return new Vector3(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        );
    }

    /**
     * Construct a new vector as the subtraction of the given parameter from this.
     * @param other The vector to add.
     * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
     */
    public Vector3 minus(Vector3 other)
    {
        return new Vector3(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z
        );
    }

    /**
     * Construct a new vector that is the negation of this.
     * @return A new vector with the values (-x, -y, -z)
     */
    public Vector3 negated()
    {
        return new Vector3(-this.x, -this.y, -this.z);
    }

    /**
     * Construct a new vector that is the product of this and a given scaler.
     * @param s The scaler to multiply by.
     * @return A new vector equal to (s*x, s*y, s*z)
     */
    public Vector3 times(float s)
    {
        return new Vector3(s*this.x, s*this.y, s*this.z);
    }

    /**
     * Construct a new vector that is the quotient of this and a given scaler.
     * @param s The scaler to divide by.
     * @return A new vector equal to (x/s, y/s, z/s)
     */
    public Vector3 dividedBy(float s)
    {
        return new Vector3(this.x/s, this.y/s, this.z/s);
    }

    /**
     * Compute the dot product (scalar product) of this vector and another given vector.
     * @param other The vector to use when computing the dot product.
     * @return A scalar value equal to the sum of x1*x2, y1*y2 and z1*z2.
     */
    public float dot(Vector3 other)
    {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    /**
     * Compute the outer product of this vector and another given vector.
     * @param other The vector to use when computing the outer product.
     * @return The matrix that is the outer product of the vectors.
     */
    public Matrix3 outerProduct(Vector3 other)
    {
        return Matrix3.fromColumns(
            new Vector3(this.x * other.x, this.y * other.x, this.z * other.x),
            new Vector3(this.x * other.y, this.y * other.y, this.z * other.y),
            new Vector3(this.x * other.z, this.y * other.z, this.z * other.z)
        );
    }

    /**
     * Compute the cross product (vector product) of this vector and another given vector.
     * @param other The vector to use when computing the dot product.
     * @return A new 3d vector with the values (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2)
     */
    public Vector3 cross(Vector3 other)
    {
        return new Vector3(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
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
    public float distance(Vector3 other)
    {
        return this.minus(other).length();
    }

    /**
     * Create a new vector with the same direction as this one but with unit
     * magnitude (a length of 1.0).  CAUTION!  May cause divide by zero error.
     * Do not attempt to normalize a zero-length vector.
     * @return A new vector equal to this vector divided by it's length.
     */
    public Vector3 normalized()
    {
        return this.times(1.0f / this.length());
    }

    public Vector3 applyOperator(DoubleUnaryOperator operator)
    {
        return new Vector3((float)operator.applyAsDouble(x), (float)operator.applyAsDouble(y), (float)operator.applyAsDouble(z));
    }

    @Override
    public String toString()
    {
        return "Vector3{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    @Override
    public Iterator<Float> iterator()
    {
        return Arrays.asList(x, y, z).iterator();
    }
}
