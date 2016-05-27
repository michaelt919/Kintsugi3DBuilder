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
 * 32-bit integers.
 * All arithmetic other than length() or distance() will be integer arithmetic, including division.  
 * This is an immutable object.
 * 
 * @author Michael Tetzlaff
 * @see Vector3
 */
public class IntVector3 
{
	/**
	 * The first dimension
	 */
	public final int x;

	/**
	 * The second dimension
	 */
	public final int y;
	
	/**
	 * The third dimension
	 */
	public final int z;

	/**
	 * Construct a vector in three dimensions with the given values.
	 * @param x Value of the first dimension.
	 * @param y Value of the second dimension.
	 * @param z Value of the third dimension.
	 */
	public IntVector3(int x, int y, int z)
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
	public IntVector3(IntVector2 v2, int z)
	{
		this(v2.x, v2.y, z);
	}
	
	/**
	 * Construct a vector in three dimensions from the given 4D vector. The
	 * fourth dimension is discarded.
	 * @param v4 The 4D vector from which the x, y and z values are copied.
	 */
	public IntVector3(IntVector4 v4)
	{
		this(v4.x, v4.y, v4.z);
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public IntVector3 plus(IntVector3 other)
	{
		return new IntVector3(
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
	public IntVector3 minus(IntVector3 other)
	{
		return new IntVector3(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z
		);
	}
	
	/**
	 * Construct a new vector that is the negation of this.
	 * @return A new vector with the values (-x, -y, -z)
	 */
	public IntVector3 negated()
	{
		return new IntVector3(-this.x, -this.y, -this.z);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scalar.
	 * @param s The scalar to multiply by.
	 * @return A new vector equal to (s*x, s*y, s*z)
	 */
	public IntVector3 times(int s)
	{
		return new IntVector3(s*this.x, s*this.y, s*this.z);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scalar.
	 * @param s The scalar to divide by.
	 * @return A new vector equal to (x/s, y/s, z/s)
	 */
	public IntVector3 dividedBy(int s)
	{
		return new IntVector3(this.x/s, this.y/s, this.z/s);
	}
	
	/**
	 * Compute the dot product (scalar product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scalar value equal to the sum of x1*x2, y1*y2 and z1*z2.
	 */
	public int dot(IntVector3 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}
	
	/**
	 * Compute the cross product (vector product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A new 3d vector with the values (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2)
	 */
	public IntVector3 cross(IntVector3 other)
	{
		return new IntVector3(
			this.y * other.z - this.z * other.y,
			this.z * other.x - this.x * other.z,
			this.x * other.y - this.y * other.x
		);
	}
	
	/**
	 * Compute a scalar value representing the Euclidean length/magnitude of this vector.
	 * @return A scalar value equal to square root of the sum of squares of the components.
	 */
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	/**
	 * Calculate the Euclidean distance between this and another given vector.
	 * @param other The vector to compute the distance between.
	 * @return A scalar value equal to the distance from the other vector.
	 */
	public double distance(IntVector3 other)
	{
		return this.minus(other).length();
	}
}
