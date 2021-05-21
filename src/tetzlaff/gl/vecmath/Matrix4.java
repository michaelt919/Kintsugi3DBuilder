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

import java.nio.FloatBuffer;

import org.lwjgl.*;

public final class Matrix4
{
    public static final Matrix4 IDENTITY = scale(1.0f);

    private final float[][] m;
    private final FloatBuffer buffer;

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

    public static Matrix4 fromColumns(Vector4 column1, Vector4 column2, Vector4 column3, Vector4 column4)
    {
        return new Matrix4(    column1.x, column2.x, column3.x, column4.x,
                            column1.y, column2.y, column3.y, column4.y,
                            column1.z, column2.z, column3.z, column4.z,
                            column1.w, column2.w, column3.w, column4.w    );
    }

    public static Matrix4 fromRows(Vector4 row1, Vector4 row2, Vector4 row3, Vector4 row4)
    {
        return new Matrix4( row1.x, row1.y, row1.z, row1.w,
                            row2.x, row2.y, row2.z, row2.w,
                            row3.x, row3.y, row3.z, row3.w,
                            row4.x, row4.y, row4.z, row4.w );
    }

    public static Matrix4 affine(Matrix3 linear, float tx, float ty, float tz)
    {
        return new Matrix4(    linear.get(0,0),    linear.get(0,1),    linear.get(0,2),    tx,
                            linear.get(1,0),    linear.get(1,1),    linear.get(1,2),    ty,
                            linear.get(2,0),    linear.get(2,1),    linear.get(2,2),    tz,
                            0.0f,                0.0f,                0.0f,                1.0f    );
    }


    public Matrix3 getUpperLeft3x3()
    {
        return Matrix3.fromRows(
                new Vector3(this.get(0,0), this.get(0,1), this.get(0,2)),
                new Vector3(this.get(1,0), this.get(1,1), this.get(1,2)),
                new Vector3(this.get(2,0), this.get(2,1), this.get(2,2)) );
    }

    public static Matrix4 fromDoublePrecision(DoubleMatrix4 m4)
    {
        return new Matrix4(    (float)m4.get(0,0),    (float)m4.get(0,1),    (float)m4.get(0,2),    (float)m4.get(0,3),
                            (float)m4.get(1,0),    (float)m4.get(1,1),    (float)m4.get(1,2),    (float)m4.get(1,3),
                            (float)m4.get(2,0),    (float)m4.get(2,1),    (float)m4.get(2,2),    (float)m4.get(2,3),
                            (float)m4.get(3,0),    (float)m4.get(3,1),    (float)m4.get(3,2),    (float)m4.get(3,3)    );
    }

    public static Matrix4 scaleAndTranslate(float sx, float sy, float sz, float tx, float ty, float tz)
    {
        return new Matrix4(    sx,     0.0f,     0.0f,     tx,
                                    0.0f,    sy,        0.0f,    ty,
                                    0.0f,    0.0f,     sz,        tz,
                                    0.0f,    0.0f,    0.0f,    1.0f    );
    }

    public static Matrix4 scale(float sx, float sy, float sz)
    {
        return scaleAndTranslate(sx, sy, sz, 0.0f, 0.0f, 0.0f);
    }

    public static Matrix4 scale(float s)
    {
        return scale(s, s, s);
    }

    public static Matrix4 translate(float tx, float ty, float tz)
    {
        return scaleAndTranslate(1.0f, 1.0f, 1.0f, tx, ty, tz);
    }

    public static Matrix4 translate(Vector3 t)
    {
        return scaleAndTranslate(1.0f, 1.0f, 1.0f, t.x, t.y, t.z);
    }

    public static Matrix4 ortho(float left, float right, float bottom, float top, float near, float far)
    {
        return new Matrix4(
            2 / (right - left),    0.0f,                0.0f,                    (right + left) / (left - right),
            0.0f,                2 / (top - bottom),    0.0f,                    (top + bottom) / (bottom - top),
            0.0f,                0.0f,                2.0f / (near - far),    (far + near) / (near - far),
            0.0f,                0.0f,                0.0f,                    1.0f
        );
    }

    public static Matrix4 ortho(float left, float right, float bottom, float top)
    {
        return ortho(left, right, bottom, top, -1.0f, 1.0f);
    }

    public static Matrix4 frustum(float left, float right, float bottom, float top, float near, float far)
    {
        return new Matrix4(
            2 * near / (right - left),    0.0f,                        (right + left) / (right - left),    0.0f,
            0.0f,                        2 * near / (top - bottom),    (top + bottom) / (top - bottom),    0.0f,
            0.0f,                        0.0f,                        (far + near) / (near - far),        2.0f * far * near / (near - far),
            0.0f,                        0.0f,                        -1.0f,                                0.0f
        );
    }

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

    public static Matrix4 rotateX(double radians)
    {
        return Matrix3.rotateX(radians).asMatrix4();
    }

    public static Matrix4 rotateY(double radians)
    {
        return Matrix3.rotateY(radians).asMatrix4();
    }

    public static Matrix4 rotateZ(double radians)
    {
        return Matrix3.rotateZ(radians).asMatrix4();
    }

    public static Matrix4 rotateAxis(Vector3 axis, double radians)
    {
        return Matrix3.rotateAxis(axis, radians).asMatrix4();
    }

    public static Matrix4 fromQuaternion(float x, float y, float z, float w)
    {
        return Matrix3.fromQuaternion(x, y, z, w).asMatrix4();
    }

    public Matrix4 plus(Matrix4 other)
    {
        return new Matrix4(
            this.m[0][0] + other.m[0][0],    this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2], this.m[0][3] + other.m[0][3],
            this.m[1][0] + other.m[1][0],    this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2], this.m[1][3] + other.m[1][3],
            this.m[2][0] + other.m[2][0],    this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2], this.m[2][3] + other.m[2][3],
            this.m[3][0] + other.m[3][0],    this.m[3][1] + other.m[3][1], this.m[3][2] + other.m[3][2], this.m[3][3] + other.m[3][3]
        );
    }

    public Matrix4 minus(Matrix4 other)
    {
        return new Matrix4(
            this.m[0][0] - other.m[0][0],    this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2], this.m[0][3] - other.m[0][3],
            this.m[1][0] - other.m[1][0],    this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2], this.m[1][3] - other.m[1][3],
            this.m[2][0] - other.m[2][0],    this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2], this.m[2][3] - other.m[2][3],
            this.m[3][0] - other.m[3][0],    this.m[3][1] - other.m[3][1], this.m[3][2] - other.m[3][2], this.m[3][3] - other.m[3][3]
        );
    }

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

    public Vector4 times(Vector4 vector)
    {
        return new Vector4(
            this.m[0][0] * vector.x + this.m[0][1] * vector.y + this.m[0][2] * vector.z + this.m[0][3] * vector.w,
            this.m[1][0] * vector.x + this.m[1][1] * vector.y + this.m[1][2] * vector.z + this.m[1][3] * vector.w,
            this.m[2][0] * vector.x + this.m[2][1] * vector.y + this.m[2][2] * vector.z + this.m[2][3] * vector.w,
            this.m[3][0] * vector.x + this.m[3][1] * vector.y + this.m[3][2] * vector.z + this.m[3][3] * vector.w
        );
    }

    public Matrix4 negate()
    {
        return new Matrix4(
            -this.m[0][0], -this.m[0][1], -this.m[0][2], -this.m[0][3],
            -this.m[1][0], -this.m[1][1], -this.m[1][2], -this.m[1][3],
            -this.m[2][0], -this.m[2][1], -this.m[2][2], -this.m[2][3],
            -this.m[3][0], -this.m[3][1], -this.m[3][2], -this.m[3][3]
        );
    }

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

    public float get(int row, int col)
    {
        return this.m[row][col];
    }

    public Vector4 getRow(int row)
    {
        return new Vector4(this.m[row][0], this.m[row][1], this.m[row][2], this.m[row][3]);
    }

    public Vector4 getColumn(int col)
    {
        return new Vector4(this.m[0][col], this.m[1][col], this.m[2][col], this.m[3][col]);
    }

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
