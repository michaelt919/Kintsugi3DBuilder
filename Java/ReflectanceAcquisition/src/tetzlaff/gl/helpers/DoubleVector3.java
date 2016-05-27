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
 * A vector of three dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 * @see Vector3
 */
public class DoubleVector3 
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
	 * Construct a vector in three dimensions from the given 2D vector and a scalar
	 * value for the missing dimension.
	 * @param v2 The 2D vector from which the x and y values are copied.
	 * @param z Value of the third dimension.
	 */
	public DoubleVector3(DoubleVector2 v2, double z)
	{
		this(v2.x, v2.y, z);
	}
	
	/**
	 * Construct a vector in three dimensions from the given 4D vector. The
	 * fourth dimension is discarded.
	 * @param v4 The 4D vector from which the x, y and z values are copied.
	 */
	public DoubleVector3(DoubleVector4 v4)
	{
		this(v4.x, v4.y, v4.z);
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public DoubleVector3 plus(DoubleVector3 other)
	{
		return new DoubleVector3(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z
		);
	}
	
	/**
	 * Construct a new vector as the subtraction of the given parameter from this.
	 * @param other The vector to subtract.
	 * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
	 */
	public DoubleVector3 minus(DoubleVector3 other)
	{
		return new DoubleVector3(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z
		);
	}
	
	/**
	 * Construct a new vector that is the negation of this.
	 * @return A new vector with the values (-x, -y, -z)
	 */
	public DoubleVector3 negated()
	{
		return new DoubleVector3(-this.x, -this.y, -this.z);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scalar.
	 * @param s The scalar to multiply by.
	 * @return A new vector equal to (s*x, s*y, s*z)
	 */
	public DoubleVector3 times(double s)
	{
		return new DoubleVector3(s*this.x, s*this.y, s*this.z);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scalar.
	 * @param s The scalar to divide by.
	 * @return A new vector equal to (x/s, y/s, z/s)
	 */
	public DoubleVector3 dividedBy(double s)
	{
		return new DoubleVector3(this.x/s, this.y/s, this.z/s);
	}
	
	/**
	 * Compute the dot product (scalar product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scalar value equal to the sum of x1*x2, y1*y2 and z1*z2.
	 */
	public double dot(DoubleVector3 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}
	
	/**
	 * Compute the cross product (vector product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A new 3d vector with the values (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2)
	 */
	public DoubleVector3 cross(DoubleVector3 other)
	{
		return new DoubleVector3(
			this.y * other.z - this.z * other.y,
			this.z * other.x - this.x * other.z,
			this.x * other.y - this.y * other.x
		);
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
	public double distance(DoubleVector3 other)
	{
		return this.minus(other).length();
	}
	
	/**
	 * Create a new vector with the same direction as this one but with unit
	 * magnitude (a length of 1.0).  
	 * Attempting to normalize a zero-length vector will result in NaN values.
	 * @return A new vector equal to this vector divided by it's length.
	 */
	public DoubleVector3 normalized()
	{
		return this.times(1.0 / this.length());
	}
}
