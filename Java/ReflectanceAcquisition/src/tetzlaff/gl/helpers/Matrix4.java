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

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

/**
 * A 4x4 matrix backed by 32-bit floats.  This is an immutable object.
 * @author Michael Tetzlaff
 *
 */
public class Matrix4 
{
	/**
	 * The matrix data.
	 */
	private float[][] m;
	
	/**
	 * A native buffer for sending this matrix to the graphics card.
	 */
	private FloatBuffer buffer;
	
	/**
	 * Creates a new matrix by specifying each entry.
	 * @param m11 The entry at row 1, column 1.
	 * @param m12 The entry at row 1, column 2.
	 * @param m13 The entry at row 1, column 3.
	 * @param m14 The entry at row 1, column 4.
	 * @param m21 The entry at row 2, column 1.
	 * @param m22 The entry at row 2, column 2.
	 * @param m23 The entry at row 2, column 3.
	 * @param m24 The entry at row 2, column 4.
	 * @param m31 The entry at row 3, column 1.
	 * @param m32 The entry at row 3, column 2.
	 * @param m33 The entry at row 3, column 3.
	 * @param m34 The entry at row 3, column 4.
	 * @param m41 The entry at row 4, column 1.
	 * @param m42 The entry at row 4, column 2.
	 * @param m43 The entry at row 4, column 3.
	 * @param m44 The entry at row 4, column 4.
	 */
	public Matrix4(
		float m11, float m12, float m13, float m14,
		float m21, float m22, float m23, float m24,
		float m31, float m32, float m33, float m34,
		float m41, float m42, float m43, float m44)
    {
        m = new float[4][4];
        m[0][0] = m11;
        m[1][0] = m21;
        m[2][0] = m31;
        m[3][0] = m41;
        m[0][1] = m12;
        m[1][1] = m22;
        m[2][1] = m32;
        m[3][1] = m42;
        m[0][2] = m13;
        m[1][2] = m23;
        m[2][2] = m33;
        m[3][2] = m43;
        m[0][3] = m14;
        m[1][3] = m24;
        m[2][3] = m34;
        m[3][3] = m44;
        
        buffer = BufferUtils.createFloatBuffer(16);
        buffer.put(m11); 
        buffer.put(m21); 
        buffer.put(m31);
        buffer.put(m41);
        buffer.put(m12);
        buffer.put(m22);
        buffer.put(m32);
        buffer.put(m42);
        buffer.put(m13);
        buffer.put(m23);
        buffer.put(m33);
        buffer.put(m43);
        buffer.put(m14);
        buffer.put(m24);
        buffer.put(m34);
        buffer.put(m44);
        buffer.flip();
    }
	
	/**
	 * Creates a 4x4 transformation matrix in homogeneous coordinates from a 3x3 transformation matrix and a translation.
	 * @param m3 The 3x3 transformation matrix.
	 * @param tx The translation along the x-axis.
	 * @param ty The translation along the y-axis.
	 * @param tz The translation along the z-axis.
	 */
	public Matrix4(Matrix3 m3, float tx, float ty, float tz)
	{
		this(	m3.get(0,0),	m3.get(0,1),	m3.get(0,2),	tx,
				m3.get(1,0),	m3.get(1,1),	m3.get(1,2),	ty,
				m3.get(2,0),	m3.get(2,1),	m3.get(2,2),	tz,
				0.0f,			0.0f,			0.0f,			1.0f	);
	}
	
	/**
	 * Creates a 4x4 matrix from a 3x3 matrix by filling the rest of the matrix with identity.
	 * @param m3 The 3x3 matrix.
	 */
	public Matrix4(Matrix3 m3)
	{
		this(m3, 0.0f, 0.0f, 0.0f);
	}

	/**
	 * Creates a scale + translation matrix.
	 * @param sx The scale along the x-axis.
	 * @param sy The scale along the y-axis.
	 * @param sz The scale along the z-axis.
	 * @param tx The translation along the x-axis.
	 * @param ty The translation along the y-axis.
	 * @param tz The translation along the z-axis.
	 */
	public Matrix4(float sx, float sy, float sz, 
			float tx, float ty, float tz) 
	{
		this(	sx, 	0.0f, 	0.0f, 	tx,
				0.0f,	sy,		0.0f,	ty,
				0.0f,	0.0f, 	sz,		tz,
				0.0f,	0.0f,	0.0f,	1.0f	);
	}

	/**
	 * Creates a scale matrix.
	 * The fourth component is always scaled by 1 (identity).
	 * @param sx The scale along the x-axis.
	 * @param sy The scale along the y-axis.
	 * @param sz The scale along the z-axis.
	 */
	public Matrix4(float sx, float sy, float sz) 
	{
		this(sx, sy, sz, 0.0f, 0.0f, 0.0f);
	}
	
	/**
	 * Creates a uniform scale matrix that preserves proportions.
	 * @param s The scale along all axes.
	 */
	public Matrix4(float s)
	{
		this(s, s, s);
	}

	/**
	 * Creates an identity matrix.
	 */
	public Matrix4() 
	{
		this(1.0f);
	}
	
	/**
	 * Gets a scale matrix.
	 * The fourth component is always scaled by 1 (identity).
	 * @param sx The scale along the x-axis.
	 * @param sy The scale along the y-axis.
	 * @param sz The scale along the z-axis.
	 * @return The specified scale matrix.
	 */
	public static Matrix4 scale(float sx, float sy, float sz)
	{
		return new Matrix4(sx, sy, sz);
	}
	
	/**
	 * Gets a uniform scale matrix that preserves proportions.
	 * @param s The scale along all axes.
	 * @return The specified scale matrix.
	 */
	public static Matrix4 scale(float s)
	{
		return new Matrix4(s);
	}
	
	/**
	 * Gets an identity matrix.
	 * @return An identity matrix.
	 */
	public static Matrix4 identity()
	{
		return new Matrix4();
	}
	
	/**
	 * Gets a translation matrix.
	 * @param tx The translation along the x-axis.
	 * @param ty The translation along the y-axis.
	 * @param tz The translation along the z-axis.
	 * @return The specified translation matrix.
	 */
	public static Matrix4 translate(float tx, float ty, float tz)
	{
		return new Matrix4(1.0f, 1.0f, 1.0f, tx, ty, tz);
	}
	
	/**
	 * Gets a translation matrix.
	 * @param t The translation vector.
	 * @return The specified translation matrix.
	 */
	public static Matrix4 translate(Vector3 t)
	{
		return new Matrix4(1.0f, 1.0f, 1.0f, t.x, t.y, t.z);
	}
	
	/**
	 * Gets an orthographic projection matrix.
	 * @param left The left clipping plane.
	 * @param right The right clipping plane.
	 * @param bottom The bottom clipping plane.
	 * @param top The top clipping plane.
	 * @param near The near clipping plane.
	 * @param far The far clipping plane.
	 * @return The specified orthographic projection matrix.
	 */
	public static Matrix4 ortho(float left, float right, float bottom, float top, float near, float far)
	{
		return new Matrix4(
			2 / (right - left),	0.0f,				0.0f,					(right + left) / (left - right),
			0.0f,				2 / (top - bottom),	0.0f,					(top + bottom) / (bottom - top),
			0.0f,				0.0f,				2.0f / (near - far),	(far + near) / (near - far),
			0.0f,				0.0f,				0.0f,					1.0f
		);
	}
	
	/**
	 * Gets an orthographic projection matrix with the near clipping plane set to -1, and the far clipping plane set to +1.
	 * @param left The left clipping plane.
	 * @param right The right clipping plane.
	 * @param bottom The bottom clipping plane.
	 * @param top The top clipping plane.
	 * @return The specified orthographic projection matrix.
	 */
	public static Matrix4 ortho(float left, float right, float bottom, float top)
	{
		return Matrix4.ortho(left, right, bottom, top, -1.0f, 1.0f);
	}
	
	/**
	 * Gets a perspective projection matrix using a particular frustum.
	 * @param left The intersection of the left clipping plane with the near clipping plane.
	 * @param right The intersection of the right clipping plane with the near clipping plane.
	 * @param bottom The intersection of the bottom clipping plane with the near clipping plane.
	 * @param top The intersection of the top clipping plane with the near clipping plane.
	 * @param near The near clipping plane.
	 * @param far The far clipping plane.
	 * @return The specified perspective projection matrix.
	 */
	public static Matrix4 frustum(float left, float right, float bottom, float top, float near, float far)
	{
		return new Matrix4(
			2 * near / (right - left),	0.0f,						(right + left) / (right - left),	0.0f,
			0.0f,						2 * near / (top - bottom),	(top + bottom) / (top - bottom),	0.0f,
			0.0f,						0.0f,						(far + near) / (near - far),		2.0f * far * near / (near - far),
			0.0f,						0.0f,						-1.0f,								0.0f
		);
	}
	
	/**
	 * Gets a perspective projection matrix with a particular aspect ratio and field of view.
	 * @param fovy The vertical field-of-view.
	 * @param aspect The aspect ratio.
	 * @param near The near clipping plane.
	 * @param far The far clipping plane.
	 * @return The specified perspective projection matrix.
	 */
	public static Matrix4 perspective(float fovy, float aspect, float near, float far)
	{
		float f = 1.0f / (float)Math.tan(fovy / 2);
		return new Matrix4(
			f / aspect,	0.0f,	0.0f,							0.0f,
			0.0f,		f,		0.0f,							0.0f,
			0.0f,		0.0f,	(far + near) / (near - far),	2.0f * far * near / (near - far),
			0.0f,		0.0f,	-1.0f,							0.0f
		);
	}
	
	/**
	 * Gets a matrix for transforming into a particular camera space.
	 * @param eye The position of the camera.
	 * @param center The point at which the camera is facing.
	 * @param up The direction considered to be pointing "up."
	 * @return The specified transformation matrix.
	 */
	public static Matrix4 lookAt(
		Vector3 eye,
		Vector3 center,
		Vector3 up)
	{
		Vector3 f = center.minus(eye).normalized();
		up = up.normalized();
		Vector3 s = f.cross(up);
		Vector3 u = s.cross(f);
		
		return new Matrix4(
			s.x, 	s.y, 	s.z, 	0.0f,
			u.x, 	u.y, 	u.z, 	0.0f,
			-f.x,	-f.y,	-f.z,	0.0f,
			0.0f,	0.0f,	0.0f,	1.0f
		).times(Matrix4.translate(-eye.x, -eye.y, -eye.z));
	}
	
	/**
	 * Gets a rotation matrix about the x-axis.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static Matrix4 rotateX(double radians)
	{
		return new Matrix4(Matrix3.rotateX(radians));
	}
	
	/**
	 * Gets a rotation matrix about the y-axis.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static Matrix4 rotateY(double radians)
	{
		return new Matrix4(Matrix3.rotateY(radians));
	}
	
	/**
	 * Gets a rotation matrix about the z-axis.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static Matrix4 rotateZ(double radians)
	{
		return new Matrix4(Matrix3.rotateZ(radians));
	}
	
	/**
	 * Gets a rotation matrix about an arbitrary axis.
	 * @param axis The axis about which to rotate.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static Matrix4 rotateAxis(Vector3 axis, double radians)
	{
		return new Matrix4(Matrix3.rotateAxis(axis, radians));
	}
	
	/**
	 * Gets a rotation matrix from a quaternion.
	 * @param x The first component of the quaternion.
	 * @param y The second component of the quaternion.
	 * @param z The third component of the quaternion.
	 * @param w The fourth (identity) component of the quaternion.
	 * @return The specified transformation matrix.
	 */
	public static Matrix4 fromQuaternion(float x, float y, float z, float w)
	{
		return new Matrix4(Matrix3.fromQuaternion(x, y, z, w));
	}
	
	/**
	 * Gets a new matrix that is the sum of this matrix and another matrix.
	 * @param other The matrix to add to this one.
	 * @return A new matrix that is the sum of the two matrices.
	 */
	public Matrix4 plus(Matrix4 other)
	{
		return new Matrix4(
			this.m[0][0] + other.m[0][0],	this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2], this.m[0][3] + other.m[0][3],
			this.m[1][0] + other.m[1][0],	this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2], this.m[1][3] + other.m[1][3],
			this.m[2][0] + other.m[2][0],	this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2], this.m[2][3] + other.m[2][3],
			this.m[3][0] + other.m[3][0],	this.m[3][1] + other.m[3][1], this.m[3][2] + other.m[3][2], this.m[3][3] + other.m[3][3]
		);
	}
	
	/**
	 * Gets a new matrix that is the difference of this matrix and another matrix.
	 * @param other The matrix to subtract from this one.
	 * @return A new matrix that is the difference of the two matrices.
	 */
	public Matrix4 minus(Matrix4 other)
	{
		return new Matrix4(
			this.m[0][0] - other.m[0][0],	this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2], this.m[0][3] - other.m[0][3],
			this.m[1][0] - other.m[1][0],	this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2], this.m[1][3] - other.m[1][3],
			this.m[2][0] - other.m[2][0],	this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2], this.m[2][3] - other.m[2][3],
			this.m[3][0] - other.m[3][0],	this.m[3][1] - other.m[3][1], this.m[3][2] - other.m[3][2], this.m[3][3] - other.m[3][3]
		);
	}
	
	/**
	 * Gets a new matrix that is the result of multiplying/transforming another matrix by this one.
	 * @param other The matrix to transform.
	 * @return A new matrix that is the result of transforming the other matrix by this one.
	 */
	public Matrix4 times(Matrix4 other)
	{
		return new Matrix4(
			this.m[0][0] * other.m[0][0] + this.m[0][1] * other.m[1][0] + this.m[0][2] * other.m[2][0] + this.m[0][3] * other.m[3][0],	
			this.m[0][0] * other.m[0][1] + this.m[0][1] * other.m[1][1] + this.m[0][2] * other.m[2][1] + this.m[0][3] * other.m[3][1],	
			this.m[0][0] * other.m[0][2] + this.m[0][1] * other.m[1][2] + this.m[0][2] * other.m[2][2] + this.m[0][3] * other.m[3][2],	
			this.m[0][0] * other.m[0][3] + this.m[0][1] * other.m[1][3] + this.m[0][2] * other.m[2][3] + this.m[0][3] * other.m[3][3],	
			this.m[1][0] * other.m[0][0] + this.m[1][1] * other.m[1][0] + this.m[1][2] * other.m[2][0] + this.m[1][3] * other.m[3][0],	
			this.m[1][0] * other.m[0][1] + this.m[1][1] * other.m[1][1] + this.m[1][2] * other.m[2][1] + this.m[1][3] * other.m[3][1],	
			this.m[1][0] * other.m[0][2] + this.m[1][1] * other.m[1][2] + this.m[1][2] * other.m[2][2] + this.m[1][3] * other.m[3][2],
			this.m[1][0] * other.m[0][3] + this.m[1][1] * other.m[1][3] + this.m[1][2] * other.m[2][3] + this.m[1][3] * other.m[3][3],
			this.m[2][0] * other.m[0][0] + this.m[2][1] * other.m[1][0] + this.m[2][2] * other.m[2][0] + this.m[2][3] * other.m[3][0],	
			this.m[2][0] * other.m[0][1] + this.m[2][1] * other.m[1][1] + this.m[2][2] * other.m[2][1] + this.m[2][3] * other.m[3][1],	
			this.m[2][0] * other.m[0][2] + this.m[2][1] * other.m[1][2] + this.m[2][2] * other.m[2][2] + this.m[2][3] * other.m[3][2],	
			this.m[2][0] * other.m[0][3] + this.m[2][1] * other.m[1][3] + this.m[2][2] * other.m[2][3] + this.m[2][3] * other.m[3][3],
			this.m[3][0] * other.m[0][0] + this.m[3][1] * other.m[1][0] + this.m[3][2] * other.m[2][0] + this.m[3][3] * other.m[3][0],	
			this.m[3][0] * other.m[0][1] + this.m[3][1] * other.m[1][1] + this.m[3][2] * other.m[2][1] + this.m[3][3] * other.m[3][1],	
			this.m[3][0] * other.m[0][2] + this.m[3][1] * other.m[1][2] + this.m[3][2] * other.m[2][2] + this.m[3][3] * other.m[3][2],	
			this.m[3][0] * other.m[0][3] + this.m[3][1] * other.m[1][3] + this.m[3][2] * other.m[2][3] + this.m[3][3] * other.m[3][3]
		);
	}
	
	/**
	 * Gets a new vector that is the result of transforming a vector by this matrix.
	 * @param vector The vector to transform.
	 * @return A new vector that is the result of transforming the original vector by this matrix.
	 */
	public Vector4 times(Vector4 vector)
	{
		return new Vector4(
			this.m[0][0] * vector.x + this.m[0][1] * vector.y + this.m[0][2] * vector.z + this.m[0][3] * vector.w,
			this.m[1][0] * vector.x + this.m[1][1] * vector.y + this.m[1][2] * vector.z + this.m[1][3] * vector.w,
			this.m[2][0] * vector.x + this.m[2][1] * vector.y + this.m[2][2] * vector.z + this.m[2][3] * vector.w,
			this.m[3][0] * vector.x + this.m[3][1] * vector.y + this.m[3][2] * vector.z + this.m[3][3] * vector.w
		);
	}
	
	/**
	 * Gets a new matrix that is the negation of this matrix.
	 * @return A new matrix with the values of this matrix, but negated.
	 */
	public Matrix4 negate()
	{
		return new Matrix4(
			-this.m[0][0], -this.m[0][1], -this.m[0][2], -this.m[0][3],
			-this.m[1][0], -this.m[1][1], -this.m[1][2], -this.m[1][3],
			-this.m[2][0], -this.m[2][1], -this.m[2][2], -this.m[2][3],
			-this.m[3][0], -this.m[3][1], -this.m[3][2], -this.m[3][3]
		);
	}
	
	/**
	 * Gets a new matrix that is the transpose of this matrix.
	 * @return A new matrix with the values of this matrix, but with rows and columns interchanged.
	 */
	public Matrix4 transpose()
	{
		return new Matrix4(
			this.m[0][0], this.m[1][0], this.m[2][0], this.m[3][0],
			this.m[0][1], this.m[1][1], this.m[2][1], this.m[3][1],
			this.m[0][2], this.m[1][2], this.m[2][2], this.m[3][2],
			this.m[0][3], this.m[1][3], this.m[2][3], this.m[3][3]
		);
	}
	
	/**
	 * Gets a particular entry of this matrix.
	 * @param row The row of the entry to retrieve.
	 * @param col The column of the entry to retrieve.
	 * @return The entry at the specified row and column.
	 */
	public float get(int row, int col)
	{
		return this.m[row][col];
	}
	
	/**
	 * Gets a particular row of the matrix.
	 * @param row The index of the row to retrieve.
	 * @return The row vector at the specified index.
	 */
	public Vector4 getRow(int row)
	{
		return new Vector4(this.m[row][0], this.m[row][1], this.m[row][2], this.m[row][3]);
	}
	
	/**
	 * Gets a particular column of the matrix.
	 * @param row The index of the row to retrieve.
	 * @return The row vector at the specified index.
	 */
	public Vector4 getColumn(int col)
	{
		return new Vector4(this.m[0][col], this.m[1][col], this.m[2][col], this.m[3][col]);
	}

	/**
	 * Gets a native buffer for sending this matrix to the graphics card.
	 * @return A native buffer containing this matrix which can be used by a GL.
	 */
	public FloatBuffer asFloatBuffer() 
	{
		return this.buffer.asReadOnlyBuffer();
	}
}
