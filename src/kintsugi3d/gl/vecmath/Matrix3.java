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

/**
 * A 3x3 matrix backed by 32-bit floats.  This is an immutable object.
 * @author Michael Tetzlaff
 *
 */
public final class Matrix3
{
    public static final Matrix3 IDENTITY = scale(1.0f);

    /**
     * The matrix data.
     */
    private final float[][] m;

    /**
     * Creates a new matrix by specifying each entry.
     * @param m11 The entry at row 1, column 1.
     * @param m12 The entry at row 1, column 2.
     * @param m13 The entry at row 1, column 3.
     * @param m21 The entry at row 2, column 1.
     * @param m22 The entry at row 2, column 2.
     * @param m23 The entry at row 2, column 3.
     * @param m31 The entry at row 3, column 1.
     * @param m32 The entry at row 3, column 2.
     * @param m33 The entry at row 3, column 3.
     */
    private Matrix3(
        float m11, float m12, float m13,
        float m21, float m22, float m23,
        float m31, float m32, float m33)
    {
        m = new float[3][3];
        m[0][0] = m11;
        m[1][0] = m21;
        m[2][0] = m31;
        m[0][1] = m12;
        m[1][1] = m22;
        m[2][1] = m32;
        m[0][2] = m13;
        m[1][2] = m23;
        m[2][2] = m33;
    }

    public static Matrix3 fromColumns(Vector3 col1, Vector3 col2, Vector3 col3)
    {
        return new Matrix3( col1.x, col2.x, col3.x,
                            col1.y, col2.y, col3.y,
                            col1.z, col2.z, col3.z );
    }

    public static Matrix3 fromRows(Vector3 row1, Vector3 row2, Vector3 row3)
    {
        return new Matrix3( row1.x, row1.y, row1.z,
                            row2.x, row2.y, row2.z,
                            row3.x, row3.y, row3.z );
    }

    public static Matrix3 fromDoublePrecision(DoubleMatrix3 m3)
    {
        return new Matrix3(    (float)m3.get(0,0),    (float)m3.get(0,1),    (float)m3.get(0,2),
                            (float)m3.get(1,0),    (float)m3.get(1,1),    (float)m3.get(1,2),
                            (float)m3.get(2,0),    (float)m3.get(2,1),    (float)m3.get(2,2)    );
    }

    /**
     * Gets a scale matrix.
     * @param sx The scale along the x-axis.
     * @param sy The scale along the y-axis.
     * @param sz The scale along the z-axis.
     * @return The specified scale matrix.
     */
    public static Matrix3 scale(float sx, float sy, float sz)
    {
        return new Matrix3(    sx,     0.0f,     0.0f,
                                    0.0f,    sy,        0.0f,
                                    0.0f,    0.0f,     sz        );
    }

    /**
     * Gets a uniform scale matrix that preserves proportions.
     * @param s The scale along all axes.
     * @return The specified scale matrix.
     */
    public static Matrix3 scale(float s)
    {
        return scale(s, s, s);
    }

    /**
     * Gets a rotation matrix about the x-axis.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix3 rotateX(double radians)
    {
        float sinTheta = (float)Math.sin(radians);
        float cosTheta = (float)Math.cos(radians);
        return new Matrix3(
            1.0f,    0.0f,         0.0f,
            0.0f,    cosTheta,    -sinTheta,
            0.0f,    sinTheta,    cosTheta
        );
    }

    /**
     * Gets a rotation matrix about the y-axis.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix3 rotateY(double radians)
    {
        float sinTheta = (float)Math.sin(radians);
        float cosTheta = (float)Math.cos(radians);
        return new Matrix3(
            cosTheta,    0.0f,     sinTheta,
            0.0f,        1.0f,    0.0f,
            -sinTheta,    0.0f,    cosTheta
        );
    }

    /**
     * Gets a rotation matrix about the z-axis.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix3 rotateZ(double radians)
    {
        float sinTheta = (float)Math.sin(radians);
        float cosTheta = (float)Math.cos(radians);
        return new Matrix3(
            cosTheta,    -sinTheta,    0.0f,
            sinTheta,    cosTheta,    0.0f,
            0.0f,        0.0f,        1.0f
        );
    }

    /**
     * Gets a rotation matrix about an arbitrary axis.
     * @param axis The axis about which to rotate.
     * @param radians The amount of rotation, in radians.
     * @return The specified transformation matrix.
     */
    public static Matrix3 rotateAxis(Vector3 axis, double radians)
    {
        float sinTheta = (float)Math.sin(radians);
        float cosTheta = (float)Math.cos(radians);
        float oneMinusCosTheta = 1.0f - cosTheta;
        return new Matrix3(

            axis.x * axis.x * oneMinusCosTheta + cosTheta,
                axis.x * axis.y * oneMinusCosTheta - axis.z * sinTheta,
                    axis.x * axis.z * oneMinusCosTheta + axis.y * sinTheta,

            axis.y * axis.x * oneMinusCosTheta + axis.z * sinTheta,
                axis.y * axis.y * oneMinusCosTheta + cosTheta,
                    axis.y * axis.z * oneMinusCosTheta - axis.x * sinTheta,

            axis.z * axis.x * oneMinusCosTheta - axis.y * sinTheta,
                axis.z * axis.y * oneMinusCosTheta + axis.x * sinTheta,
                    axis.z * axis.z * oneMinusCosTheta + cosTheta
        );
    }

    /**
     * Gets a rotation matrix from a quaternion.
     * @param x The first component of the quaternion.
     * @param y The second component of the quaternion.
     * @param z The third component of the quaternion.
     * @param w The fourth (identity) component of the quaternion.
     * @return The specified transformation matrix.
     */
    public static Matrix3 fromQuaternion(float x, float y, float z, float w)
    {
        return new Matrix3(
            1 - 2*y*y - 2*z*z,    2*x*y - 2*z*w,        2*x*z + 2*y*w,
            2*x*y + 2*z*w,        1 - 2*x*x - 2*z*z,    2*y*z - 2*x*w,
            2*x*z - 2*y*w,        2*y*z + 2*x*w,        1 - 2*x*x - 2*y*y
        );
    }

    /**
     * Convert the given matrix (assumed to be a rotation matrix) to a quaternion.
     * @return The quaternion packed in a Vector4
     */
    public Vector4 toQuaternion()
    {
        // Convert rotation matrix to quaternion
        double[] q = new double[4];
        double trace = get(0,0) + get(1,1) + get(2,2);
        if (trace > 0)
        {
            double s = 0.5 / Math.sqrt(trace + 1.0);
            q[3] = 0.25 / s;
            q[0] = (get(1,2) - get(2,1)) * s;
            q[1] = (get(2,0) - get(0,2)) * s;
            q[2] = (get(0,1) - get(1,0)) * s;
        }
        else
        {
            if (get(0,0) > get(1,1) && get(0,0) > get(2,2))
            {
                double s = 2.0 * Math.sqrt(0.0 + get(0,0) - get(1,1) - get(2,2));
                q[3] = (get(1,2) - get(2,1)) / s;
                q[0] = 0.25 * s;
                q[1] = (get(1,0) + get(0,1)) / s;
                q[2] = (get(2,0) + get(0,2)) / s;
            }
            else if (get(1,1) > get(2,2))
            {
                double s = 2.0 * Math.sqrt(0.0 + get(1,1) - get(0,0) - get(2,2));
                q[3] = (get(2,0) - get(0,2)) / s;
                q[0] = (get(1,0) + get(0,1)) / s;
                q[1] = 0.25 * s;
                q[2] = (get(2,1) + get(1,2)) / s;
            }
            else
            {
                double s = 2.0 * Math.sqrt(0.0 + get(2,2) - get(0,0) - get(1,1));
                q[3] = (get(0,1) - get(1,0)) / s;
                q[0] = (get(2,0) + get(0,2)) / s;
                q[1] = (get(2,1) + get(1,2)) / s;
                q[2] = 0.25 * s;
            }
        }
        
        return new Vector4((float)q[0], (float)q[1], (float)q[2], (float)q[3]);
    }

    public Matrix4 asMatrix4()
    {
        return Matrix4.affine(this, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Gets a new matrix that is the sum of this matrix and another matrix.
     * @param other The matrix to add to this one.
     * @return A new matrix that is the sum of the two matrices.
     */
    public Matrix3 plus(Matrix3 other)
    {
        return new Matrix3(
            this.m[0][0] + other.m[0][0],    this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2],
            this.m[1][0] + other.m[1][0],    this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2],
            this.m[2][0] + other.m[2][0],    this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2]
        );
    }

    /**
     * Gets a new matrix that is the difference of this matrix and another matrix.
     * @param other The matrix to subtract from this one.
     * @return A new matrix that is the difference of the two matrices.
     */
    public Matrix3 minus(Matrix3 other)
    {
        return new Matrix3(
            this.m[0][0] - other.m[0][0],    this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2],
            this.m[1][0] - other.m[1][0],    this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2],
            this.m[2][0] - other.m[2][0],    this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2]
        );
    }

    /**
     * Gets a new matrix that is the result of multiplying/transforming another matrix by this one.
     * @param other The matrix to transform.
     * @return A new matrix that is the result of transforming the other matrix by this one.
     */
    public Matrix3 times(Matrix3 other)
    {
        return new Matrix3(
            this.m[0][0] * other.m[0][0] + this.m[0][1] * other.m[1][0] + this.m[0][2] * other.m[2][0],
            this.m[0][0] * other.m[0][1] + this.m[0][1] * other.m[1][1] + this.m[0][2] * other.m[2][1],
            this.m[0][0] * other.m[0][2] + this.m[0][1] * other.m[1][2] + this.m[0][2] * other.m[2][2],
            this.m[1][0] * other.m[0][0] + this.m[1][1] * other.m[1][0] + this.m[1][2] * other.m[2][0],
            this.m[1][0] * other.m[0][1] + this.m[1][1] * other.m[1][1] + this.m[1][2] * other.m[2][1],
            this.m[1][0] * other.m[0][2] + this.m[1][1] * other.m[1][2] + this.m[1][2] * other.m[2][2],
            this.m[2][0] * other.m[0][0] + this.m[2][1] * other.m[1][0] + this.m[2][2] * other.m[2][0],
            this.m[2][0] * other.m[0][1] + this.m[2][1] * other.m[1][1] + this.m[2][2] * other.m[2][1],
            this.m[2][0] * other.m[0][2] + this.m[2][1] * other.m[1][2] + this.m[2][2] * other.m[2][2]
        );
    }

    /**
     * Gets a new vector that is the result of transforming a vector by this matrix.
     * @param vector The vector to transform.
     * @return A new vector that is the result of transforming the original vector by this matrix.
     */
    public Vector3 times(Vector3 vector)
    {
        return new Vector3(
            this.m[0][0] * vector.x + this.m[0][1] * vector.y + this.m[0][2] * vector.z,
            this.m[1][0] * vector.x + this.m[1][1] * vector.y + this.m[1][2] * vector.z,
            this.m[2][0] * vector.x + this.m[2][1] * vector.y + this.m[2][2] * vector.z
        );
    }

    /**
     * Gets a new matrix that is the result of scaling this matrix.
     * @param factor The factor by which to scale.
     * @return A new matrix that is the result of scaling this matrix by the specified factor.
     */
    public Matrix3 times(float factor)
    {
        return new Matrix3(
            this.m[0][0] * factor, this.m[0][1] * factor, this.m[0][2] * factor,
            this.m[1][0] * factor, this.m[1][1] * factor, this.m[1][2] * factor,
            this.m[2][0] * factor, this.m[2][1] * factor, this.m[2][2] * factor
        );
    }

    /**
     * Gets a new matrix that is the negation of this matrix.
     * @return A new matrix with the values of this matrix, but negated.
     */
    public Matrix3 negate()
    {
        return new Matrix3(
            -this.m[0][0], -this.m[0][1], -this.m[0][2],
            -this.m[1][0], -this.m[1][1], -this.m[1][2],
            -this.m[2][0], -this.m[2][1], -this.m[2][2]
        );
    }

    /**
     * Gets a new matrix that is the transpose of this matrix.
     * @return A new matrix with the values of this matrix, but with rows and columns interchanged.
     */
    public Matrix3 transpose()
    {
        return new Matrix3(
            this.m[0][0], this.m[1][0], this.m[2][0],
            this.m[0][1], this.m[1][1], this.m[2][1],
            this.m[0][2], this.m[1][2], this.m[2][2]
        );
    }

    /**
     * Gets the determinant of this matrix.
     * @return The matrix determinant.
     */
    public float determinant()
    {
        return this.m[0][0] * (this.m[1][1] * this.m[2][2] - this.m[2][1] * this.m[1][2])
                - this.m[0][1] * (this.m[1][0] * this.m[2][2] - this.m[2][0] * this.m[1][2])
                + this.m[0][2] * (this.m[1][0] * this.m[2][1] - this.m[2][0] * this.m[1][1]);
    }

    /**
     * Gets the inverse of this matrix.
     * @return The matrix inverse.
     */
    public Matrix3 inverse()
    {
        float det = this.determinant();

        return new Matrix3(
            (m[1][1] * m[2][2] - m[1][2] * m[2][1]) / det, (m[0][2] * m[2][1] - m[0][1] * m[2][2]) / det, (m[0][1] * m[1][2] - m[0][2] * m[1][1]) / det,
            (m[1][2] * m[2][0] - m[1][0] * m[2][2]) / det, (m[0][0] * m[2][2] - m[0][2] * m[2][0]) / det, (m[0][2] * m[1][0] - m[0][0] * m[1][2]) / det,
            (m[1][0] * m[2][1] - m[1][1] * m[2][0]) / det, (m[0][1] * m[2][0] - m[0][0] * m[2][1]) / det, (m[0][0] * m[1][1] - m[0][1] * m[1][0]) / det);
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
    public Vector3 getRow(int row)
    {
        return new Vector3(this.m[row][0], this.m[row][1], this.m[row][2]);
    }

    /**
     * Gets a particular column of the matrix.
     * @param col The index of the column to retrieve.
     * @return The column vector at the specified index.
     */
    public Vector3 getColumn(int col)
    {
        return new Vector3(this.m[0][col], this.m[1][col], this.m[2][col]);
    }
}
