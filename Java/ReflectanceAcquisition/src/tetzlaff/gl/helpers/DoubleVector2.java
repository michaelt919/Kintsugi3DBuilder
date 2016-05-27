/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl.helpers;

/**
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  This is an immutable object.
 * 
 * @see Vector2
 * @author Michael Tetzlaff
 */
public class DoubleVector2 
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
	 * Construct a vector in two dimensions from the given 3D vector. The
	 * third dimension is discarded.
	 * @param v3 The 3D vector from which the x and y values are copied.
	 */
	public DoubleVector2(DoubleVector3 v3)
	{
		this(v3.x, v3.y);
	}
	
	/**
	 * Construct a vector in two dimensions from the given 4D vector. The
	 * third and fourth dimensions are discarded.
	 * @param v4 The 4D vector from which the x and y values are copied.
	 */
	public DoubleVector2(DoubleVector4 v4)
	{
		this(v4.x, v4.y);
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public DoubleVector2 plus(DoubleVector2 other)
	{
		return new DoubleVector2(
			this.x + other.x,
			this.y + other.y
		);
	}
	
	/**
	 * Construct a new vector as the subtraction of the given parameter from this.
	 * @param other The vector to subtract.
	 * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
	 */
	public DoubleVector2 minus(DoubleVector2 other)
	{
		return new DoubleVector2(
			this.x - other.x,
			this.y - other.y
		);
	}
	
	/**
	 * Construct a new vector that is the negation of this.
	 * @return A new vector with the values (-x, -y)
	 */
	public DoubleVector2 negated()
	{
		return new DoubleVector2(-this.x, -this.y);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scalar.
	 * @param s The scalar to multiply by.
	 * @return A new vector equal to (s*x, s*y)
	 */
	public DoubleVector2 times(double s)
	{
		return new DoubleVector2(s*this.x, s*this.y);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scalar.
	 * @param s The scalar to divide by.
	 * @return A new vector equal to (x/s, y/s)
	 */
	public DoubleVector2 dividedBy(double s)
	{
		return new DoubleVector2(this.x/s, this.y/s);
	}
	
	/**
	 * Compute the dot product (scalar product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scalar value equal to the sum of x1*x2 and y1*y2.
	 */
	public double dot(DoubleVector2 other)
	{
		return this.x * other.x + this.y * other.y;
	}
	
	/**
	 * Compute a scalar value representing the length/magnitude of this vector.
	 * @return A scalar value equal to square root of the sum of squares of the components.
	 */
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	/**
	 * Calculate the distance between this and another given vector.
	 * @param other The vector to compute the distance between.
	 * @return A scalar value equal to the distance from the other vector.
	 */
	public double distance(DoubleVector2 other)
	{
		return this.minus(other).length();
	}
	
	/**
	 * Create a new vector with the same direction as this one but with unit
	 * magnitude (a length of 1.0).  
	 * Attempting to normalize a zero-length vector will result in NaN values.
	 * @return A new vector equal to this vector divided by it's length.
	 */
	public DoubleVector2 normalized()
	{
		return this.times(1.0 / this.length());
	}
}
