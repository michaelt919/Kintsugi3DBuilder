/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.vecmath;

import java.nio.FloatBuffer;

import org.lwjgl.*;

/**
 * A 4x4 matrix backed by 32-bit floats.  This is an immutable object.
 * @author Michael Tetzlaff
 */
public final class Matrix4
{
    /**
     * The 4x4 identity matrix.
     */
    public static final Matrix4 IDENTITY = scale(1.0f);

    /**
     * The matrix data.
     */
    private final float[][] m;

    private final FloatBuffer buffer;

    /**
     * Creates a new matrix by specifying each entry
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
    private Matrix4(
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
     * Creates a matrix from four column vectors.
     * @param column1 The first column of the matrix.
     * @param column2 The second column of the matrix.
     * @param column3 The third column of the matrix.
     * @param column4 The fourth column of the matrix.
     * @return The new matrix.
     */
    public static Matrix4 fromColumns(Vector4 column1, Vector4 column2, Vector4 column3, Vector4 column4)
    {
        return new Matrix4(    column1.x, column2.x, column3.x, column4.x,
                            column1.y, column2.y, column3.y, column4.y,
                            column1.z, column2.z, column3.z, column4.z,
                            column1.w, column2.w, column3.w, column4.w    );
    }

    /**
     * Creates a matrix from four row vectors.
     * @param row1 The first row of the matrix.
     * @param row2 The second row of the matrix.
     * @param row3 The third row of the matrix.
     * @param row4 The fourth row of the matrix.
     * @return The new matrix.
     */
    public static Matrix4 fromRows(Vector4 row1, Vector4 row2, Vector4 row3, Vector4 row4)
    {
        return new Matrix4( row1.x, row1.y, row1.z, row1.w,
                            row2.x, row2.y, row2.z, row2.w,
                            row3.x, row3.y, row3.z, row3.w,
                            row4.x, row4.y, row4.z, row4.w );
    }

    /**
     * Creates a matrix for an affine transformation from a 3x3 linear transformation matrix and translation coefficients.
     * @param linear The linear transformation matrix.
     * @param tx The translation along the x-axis.
     * @param ty The translation along the y-axis.
     * @param tz The translation along the z-axis.
     * @return
     */
    public static Matrix4 affine(Matrix3 linear, float tx, float ty, float tz)
    {
        return new Matrix4(    linear.get(0,0),    linear.get(0,1),    linear.get(0,2),    tx,
                            linear.get(1,0),    linear.get(1,1),    linear.get(1,2),    ty,
                            linear.get(2,0),    linear.get(2,1),    linear.get(2,2),    tz,
                            0.0f,                0.0f,                0.0f,                1.0f    );
    }

    /**
     * Gets the upper-left 3x3 submatrix.
     * @return The upper-left 3x3 matrix.
     */
    public Matrix3 getUpperLeft3x3()
    {
        return Matrix3.fromRows(
                new Vector3(this.get(0,0), this.get(0,1), this.get(0,2)),
                new Vector3(this.get(1,0), this.get(1,1), this.get(1,2)),
                new Vector3(this.get(2,0), this.get(2,1), this.get(2,2)) );
    }

    /**
     * Converts a double-precision matrix to a single-precision matrix.
     * @param m4 The double-precision matrix.
     * @return The single-precision matrix.
     */
    public static Matrix4 fromDoublePrecision(DoubleMatrix4 m4)
    {
        return new Matrix4(    (float)m4.get(0,0),    (float)m4.get(0,1),    (float)m4.get(0,2),    (float)m4.get(0,3),
                            (float)m4.get(1,0),    (float)m4.get(1,1),    (float)m4.get(1,2),    (float)m4.get(1,3),
                            (float)m4.get(2,0),    (float)m4.get(2,1),    (float)m4.get(2,2),    (float)m4.get(2,3),
                            (float)m4.get(3,0),    (float)m4.get(3,1),    (float)m4.get(3,2),    (float)m4.get(3,3)    );
    }

    /**
     * Gets a matrix for a combination of scale and translation.
     * @param sx The scale along the x-axis.
     * @param sy The scale along the y-axis.
     * @param sz The scale along the z-axis.
     * @param tx The translation along the x-axis.
     * @param ty The translation along the y-axis.
     * @param tz The translation along the z-axis.
     * @return The specified translation matrix.
     */
    public static Matrix4 scaleAndTranslate(float sx, float sy, float sz, float tx, float ty, float tz)
    {
        return new Matrix4(    sx,     0.0f,     0.0f,     tx,
                                    0.0f,    sy,        0.0f,    ty,
                                    0.0f,    0.0f,     sz,        tz,
                                    0.0f,    0.0f,    0.0f,    1.0f    );
    }

    /**
     * Gets a scale matrix.
     * @param sx The scale along the x-axis.
     * @param sy The scale along the y-axis.
     * @param sz The scale along the z-axis.
     * @return The specified scale matrix.
     */
    public static Matrix4 scale(float sx, float sy, float sz)
    {
        return scaleAndTranslate(sx, sy, sz, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Gets a uniform scale matrix that preserves proportions.
     * @param s The scale along all axes.
     * @return The specified scale matrix.
     */
    public static Matrix4 scale(float s)
    {
        return scale(s, s, s);
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
        return scaleAndTranslate(1.0f, 1.0f, 1.0f, tx, ty, tz);
    }

    /**
     * Gets a translation matrix.
     * @param t The translation vector.
     * @return The specified translation matrix.
     */
    public static Matrix4 translate(Vector3 t)
    {
        return scaleAndTranslate(1.0f, 1.0f, 1.0f, t.x, t.y, t.z);
    }

    /**
     * Creates an orthographic projection matrix.
     * @param left The left clipping plane.
     * @param right The right clipping plane.
     * @param bottom The bottom clipping plane.
     * @param top The top clipping plane.
     * @param near The near clipping plane.
     * @param far The far clipping plane.
     * @return The specified projection matrix.
     */
    public static Matrix4 ortho(float left, float right, float bottom, float top, float near, float far)
    {
        return new Matrix4(
            2 / (right - left),    0.0f,                0.0f,                    (right + left) / (left - right),
            0.0f,                2 / (top - bottom),    0.0f,                    (top + bottom) / (bottom - top),
            0.0f,                0.0f,                2.0f / (near - far),    (far + near) / (near - far),
            0.0f,                0.0f,                0.0f,                    1.0f
        );
    }

    /**
     * Creates an orthographic projection matrix with default near and far planes (near=-1, far=1).
     * @param left The left clipping plane.
     * @param right The right clipping plane.
     * @param bottom The bottom clipping plane.
     * @param top The top clipping plane.
     * @return The specified projection matrix.
     */
    public static Matrix4 ortho(float left, float right, float bottom, float top)
    {
        return ortho(left, right, bottom, top, -1.0f, 1.0f);
    }

    /**
     * Creates a frustum projection matrix.
     * @param left The left clipping plane.
     * @param right The right clipping plane.
     * @param bottom The bottom clipping plane.
     * @param top The top clipping plane.
     * @param near The near clipping plane.
     * @param far The far clipping plane.
     * @return The specified projection matrix.
     */
    public static Matrix4 frustum(float left, float right, float bottom, float top, float near, float far)
    {
        return new Matrix4(
            2 * near / (right - left),    0.0f,                        (right + left) / (right - left),    0.0f,
            0.0f,                        2 * near / (top - bottom),    (top + bottom) / (top - bottom),    0.0f,
            0.0f,                        0.0f,                        (far + near) / (near - far),        2.0f * far * near / (near - far),
            0.0f,                        0.0f,                        -1.0f,                                0.0f
        );
    }

    /**
     * Creates a perspective projection matrix.
     * @param fovy The vertical field of view, in radians.
     * @param aspect The aspect ratio.
     * @param near The near clipping plane.
     * @param far The far clipping plane.
     * @return The specified projection matrix.
     */
    public static Matrix4 perspective(float fovy, float aspect, float near, float far)
    {
        float f = 1.0f / (float)Math.tan(fovy / 2);
        return new Matrix4(
            f / aspect,  0.0f,  0.0f,                         0.0f,
            0.0f,        f,     0.0f,                         0.0f,
            0.0f,        0.0f,  (far + near) / (near - far),  2.0f * far * near / (near - far),
            0.0f,        0.0f,  -1.0f,                        0.0f
        );
    }

    /**
     * Creates a matrix that represents the view transformation for a camera that is looking at a particular target.
     * @param eye The location of the camera.
     * @param center The location the camera is looking at.
     * @param up The direction that is straight up in the current world space.
     * @return The specified view matrix.
     */
    public static Matrix4 lookAt(
        Vector3 eye,
        Vector3 center,
        Vector3 up)
    {
        Vector3 f = center.minus(eye).normalized();
        Vector3 upNormalized = up.normalized();
        Vector3 s = f.cross(upNormalized).normalized();
        Vector3 u = s.cross(f).normalized();

        return new Matrix4(
            s.x,     s.y,     s.z,     0.0f,
            u.x,     u.y,     u.z,     0.0f,
            -f.x,    -f.y,    -f.z,    0.0f,
            0.0f,    0.0f,    0.0f,    1.0f
        ).times(translate(-eye.x, -eye.y, -eye.z));
    }

    /**
     * Creates a matrix that represents the view transformation for a camera that is looking at a particular target.
     * @param eyeX The location of the camera along the x-axis.
     * @param eyeY The location of the camera along the y-axis.
     * @param eyeZ The location of the camera along the z-axis.
     * @param centerX The location the camera is looking at along the x-axis.
     * @param centerY The location the camera is looking at along the y-axis.
     * @param centerZ The location the camera is looking at along the z-axis.
     * @param upX The x-component of the direction that is straight up in the current world space.
     * @param upY The y-component of the direction that is straight up in the current world space.
     * @param upZ The z-component of the direction that is straight up in the current world space.
     * @return The specified view matrix.
     */
    public static Matrix4 lookAt(
        float eyeX, float eyeY, float eyeZ,
        float centerX, float centerY, float centerZ,
        float upX, float upY, float upZ)
    {
        return lookAt(
            new Vector3(eyeX, eyeY, eyeZ),
            new Vector3(centerX, centerY, centerZ),
            new Vector3(upX, upY, upZ)
        );
    }

    /**
     * Gets an affine rotation matrix about the x-axis.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix4 rotateX(double radians)
    {
        return Matrix3.rotateX(radians).asMatrix4();
    }

    /**
     * Gets an affine rotation matrix about the y-axis.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix4 rotateY(double radians)
    {
        return Matrix3.rotateY(radians).asMatrix4();
    }

    /**
     * Gets an affine rotation matrix about the z-axis.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix4 rotateZ(double radians)
    {
        return Matrix3.rotateZ(radians).asMatrix4();
    }

    /**
     * Gets an affine rotation matrix about an arbitrary axis.
     * @param axis The axis about which to rotate.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix4 rotateAxis(Vector3 axis, double radians)
    {
        return Matrix3.rotateAxis(axis, radians).asMatrix4();
    }

    /**
     * Gets an affine rotation matrix from a quaternion.
     * @param x The first component of the quaternion.
     * @param y The second component of the quaternion.
     * @param z The third component of the quaternion.
     * @param w The fourth (identity) component of the quaternion.
     * @return The specified transformation matrix.
     */
    public static Matrix4 fromQuaternion(float x, float y, float z, float w)
    {
        return Matrix3.fromQuaternion(x, y, z, w).asMatrix4();
    }

    /**
     * Gets a new matrix that is the sum of this matrix and another matrix.
     * @param other The matrix to add to this one.
     * @return A new matrix that is the sum of the two matrices.
     */
    public Matrix4 plus(Matrix4 other)
    {
        return new Matrix4(
            this.m[0][0] + other.m[0][0],    this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2], this.m[0][3] + other.m[0][3],
            this.m[1][0] + other.m[1][0],    this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2], this.m[1][3] + other.m[1][3],
            this.m[2][0] + other.m[2][0],    this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2], this.m[2][3] + other.m[2][3],
            this.m[3][0] + other.m[3][0],    this.m[3][1] + other.m[3][1], this.m[3][2] + other.m[3][2], this.m[3][3] + other.m[3][3]
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
            this.m[0][0] - other.m[0][0],    this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2], this.m[0][3] - other.m[0][3],
            this.m[1][0] - other.m[1][0],    this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2], this.m[1][3] - other.m[1][3],
            this.m[2][0] - other.m[2][0],    this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2], this.m[2][3] - other.m[2][3],
            this.m[3][0] - other.m[3][0],    this.m[3][1] - other.m[3][1], this.m[3][2] - other.m[3][2], this.m[3][3] - other.m[3][3]
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
     * Performs a quick inverse of the matrix if the upper-left 3x3 is the product of an orthogonal (rotation) matrix and uniform scaling, and the final row is identity.
     * Throws an exception if this condition is not met (up to a specified tolerance).
     * @param tolerance A tolerance for the accuracy of the inverse matrix.
     * @return The inverse of the matrix.
     */
    public Matrix4 quickInverse(float tolerance)
    {
        Matrix3 rotationScale = this.getUpperLeft3x3();
        float scaleSquared = (float)Math.pow(rotationScale.determinant(), 2.0 / 3.0);

        Matrix4 invCandidate = rotationScale.transpose().times(1.0f / scaleSquared).asMatrix4()
                .times(translate(this.getColumn(3).getXYZ().negated()));

        Matrix4 identityCandidate = this.times(invCandidate);

        float translationScale = this.getColumn(3).getXYZ().length();

        if (Math.abs(identityCandidate.get(0, 0) - 1.0f) > tolerance ||
            Math.abs(identityCandidate.get(1, 1) - 1.0f) > tolerance ||
            Math.abs(identityCandidate.get(2, 2) - 1.0f) > tolerance ||
            Math.abs(identityCandidate.get(3, 3) - 1.0f) > tolerance ||
            Math.abs(identityCandidate.get(0, 1)) > tolerance ||
            Math.abs(identityCandidate.get(0, 2)) > tolerance ||
            Math.abs(identityCandidate.get(0, 3)) > tolerance * translationScale ||
            Math.abs(identityCandidate.get(1, 0)) > tolerance ||
            Math.abs(identityCandidate.get(1, 2)) > tolerance ||
            Math.abs(identityCandidate.get(1, 3)) > tolerance * translationScale ||
            Math.abs(identityCandidate.get(2, 0)) > tolerance ||
            Math.abs(identityCandidate.get(2, 1)) > tolerance ||
            Math.abs(identityCandidate.get(2, 3)) > tolerance * translationScale ||
            Math.abs(identityCandidate.get(3, 0)) > tolerance ||
            Math.abs(identityCandidate.get(3, 1)) > tolerance ||
            Math.abs(identityCandidate.get(3, 2)) > tolerance)
        {
            throw new IllegalStateException("A quick inverse cannot be taken.");
        }

        return invCandidate;
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
     * @param col The index of the column to retrieve.
     * @return The column vector at the specified index.
     */
    public Vector4 getColumn(int col)
    {
        return new Vector4(this.m[0][col], this.m[1][col], this.m[2][col], this.m[3][col]);
    }

    /**
     * Gets a native memory buffer containing the contents of this matrix.
     * This is useful for sending the matrix to the graphics card.
     * @return The native memory buffer containing the matrix data.
     */
    public FloatBuffer asFloatBuffer()
    {
        return this.buffer.asReadOnlyBuffer();
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 4; i++)
        {
            s.append(getRow(i)).append('\n');
        }
        return s.toString();
    }

    public void print()
    {
        System.out.println(this);
    }
}
