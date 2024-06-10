/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
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
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 32-bit floats.  This is an immutable object.
 *
 * @author Michael Tetzlaff
 */
public class Vector2 implements Iterable<Float>
{
    public static final Vector2 ZERO = new Vector2(0.0f);

    /**
     * The first dimension
     */
    public final float x;
    /**
     * The second dimension
     */
    public final float y;

    /**
     * Construct a vector in two dimensions with the given values.
     * @param x Value of the first dimension.
     * @param y Value of the second dimension.
     */
    public Vector2(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Construct a vector in two dimensions with the given values.
     * @param value Value of both dimensions.
     */
    public Vector2(float value)
    {
        this(value, value);
    }

    public Vector3 asVector3()
    {
        return asVector3(0.0f);
    }

    public Vector3 asVector3(float z)
    {
        return new Vector3(this.x, this.y, z);
    }

    public Vector4 asPosition()
    {
        return asVector4(0.0f, 1.0f);
    }

    public Vector4 asDirection()
    {
        return asVector4(0.0f, 0.0f);
    }

    public Vector4 asVector4(float z, float w)
    {
        return new Vector4(this.x, this.y, z, w);
    }

    public IntVector2 rounded()
    {
        return new IntVector2(Math.round(this.x), Math.round(this.y));
    }

    public IntVector2 truncated()
    {
        return new IntVector2((int)this.x, (int)this.y);
    }

    public DoubleVector2 asDoublePrecision()
    {
        return new DoubleVector2(x, y);
    }

    @Override
    @SuppressWarnings("FloatingPointEquality")
    public boolean equals(Object obj)
    {
        if (obj instanceof Vector2)
        {
            Vector2 other = (Vector2) obj;
            return other.x == this.x && other.y == this.y;
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
            return 31 * result + Float.floatToIntBits(y);
        }
    }

    /**
     * Construct a new vector as the sum of this one and the given parameter.
     * @param other The vector to add.
     * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
     */
    public Vector2 plus(Vector2 other)
    {
        return new Vector2(
            this.x + other.x,
            this.y + other.y
        );
    }

    /**
     * Construct a new vector as the subtraction of the given parameter from this.
     * @param other The vector to add.
     * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
     */
    public Vector2 minus(Vector2 other)
    {
        return new Vector2(
            this.x - other.x,
            this.y - other.y
        );
    }

    /**
     * Construct a new vector that is the negation of this.
     * @return A new vector with the values (-x, -y)
     */
    public Vector2 negated()
    {
        return new Vector2(-this.x, -this.y);
    }

    /**
     * Construct a new vector that is the product of this and a given scaler.
     * @param s The scaler to multiply by.
     * @return A new vector equal to (s*x, s*y)
     */
    public Vector2 times(float s)
    {
        return new Vector2(s*this.x, s*this.y);
    }

    /**
     * Construct a new vector that is the quotient of this and a given scaler.
     * @param s The scaler to divide by.
     * @return A new vector equal to (x/s, y/s)
     */
    public Vector2 dividedBy(float s)
    {
        return new Vector2(this.x/s, this.y/s);
    }

    public Vector2 times(Vector2 other)
    {
        return new Vector2(this.x * other.x, this.y * other.y);
    }

    public Vector2 dividedBy(Vector2 other)
    {
        return new Vector2(this.x / other.x, this.y / other.y);
    }

    /**
     * Compute the dot product (scaler product) of this vector and another given vector.
     * @param other The vector to use when computing the dot product.
     * @return A scaler value equal to the sum of x1*x2 and y1*y2.
     */
    public float dot(Vector2 other)
    {
        return this.x * other.x + this.y * other.y;
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
    public float distance(Vector2 other)
    {
        return this.minus(other).length();
    }

    /**
     * Create a new vector with the same direction as this one but with unit
     * magnitude (a length of 1.0).  CAUTION!  May cause divide by zero error.
     * Do not attempt to normalize a zero-length vector.
     * @return A new vector equal to this vector divided by it's length.
     */
    public Vector2 normalized()
    {
        return this.times(1.0f / this.length());
    }

    public Vector2 applyOperator(DoubleUnaryOperator operator)
    {
        return new Vector2((float)operator.applyAsDouble(x), (float)operator.applyAsDouble(y));
    }

    @Override
    public String toString()
    {
        return "Vector2{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public Iterator<Float> iterator()
    {
        return Arrays.asList(x, y).iterator();
    }
}
