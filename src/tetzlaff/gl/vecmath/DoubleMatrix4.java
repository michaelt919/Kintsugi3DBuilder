package tetzlaff.gl.vecmath;

import java.nio.DoubleBuffer;

import org.lwjgl.*;

public final class DoubleMatrix4
{
    public static final DoubleMatrix4 IDENTITY = scale(1.0);

    private final double[][] m;
    private final DoubleBuffer buffer;

    private DoubleMatrix4(
        double m11, double m12, double m13, double m14,
        double m21, double m22, double m23, double m24,
        double m31, double m32, double m33, double m34,
        double m41, double m42, double m43, double m44)
    {
        m = new double[4][4];
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
        
        buffer = BufferUtils.createDoubleBuffer(16);
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

    public static DoubleMatrix4 fromColumns(DoubleVector4 column1, DoubleVector4 column2, DoubleVector4 column3, DoubleVector4 column4)
    {
        return new DoubleMatrix4(    column1.x, column2.x, column3.x, column4.x,
                                    column1.y, column2.y, column3.y, column4.y,
                                    column1.z, column2.z, column3.z, column4.z,
                                    column1.w, column2.w, column3.w, column4.w    );
    }

    public static DoubleMatrix4 fromRows(DoubleVector4 row1, DoubleVector4 row2, DoubleVector4 row3, DoubleVector4 row4)
    {
        return new DoubleMatrix4(     row1.x, row1.y, row1.z, row1.w,
                                    row2.x, row2.y, row2.z, row2.w,
                                    row3.x, row3.y, row3.z, row3.w,
                                    row4.x, row4.y, row4.z, row4.w    );
    }

    public static DoubleMatrix4 affine(DoubleMatrix3 linear, double tx, double ty, double tz)
    {
        return new DoubleMatrix4(    linear.get(0,0),    linear.get(0,1),    linear.get(0,2),    tx,
                                    linear.get(1,0),    linear.get(1,1),    linear.get(1,2),    ty,
                                    linear.get(2,0),    linear.get(2,1),    linear.get(2,2),    tz,
                                    0.0,                0.0,                0.0,                1.0    );
    }

    public static DoubleMatrix4 fromSinglePrecision(Matrix4 m4)
    {
        return new DoubleMatrix4(    m4.get(0,0),    m4.get(0,1),    m4.get(0,2),    m4.get(0,3),
                                    m4.get(1,0),    m4.get(1,1),    m4.get(1,2),    m4.get(1,3),
                                    m4.get(2,0),    m4.get(2,1),    m4.get(2,2),    m4.get(2,3),
                                    m4.get(3,0),    m4.get(3,1),    m4.get(3,2),    m4.get(3,3)    );
    }

    public static DoubleMatrix4 scaleAndTranslate(double sx, double sy, double sz, double tx, double ty, double tz)
    {
        return new DoubleMatrix4(    sx,     0.0,     0.0,     tx,
                                    0.0,    sy,        0.0,    ty,
                                    0.0,    0.0,     sz,        tz,
                                    0.0,    0.0,    0.0,    1.0    );
    }

    public static DoubleMatrix4 scale(double sx, double sy, double sz)
    {
        return scaleAndTranslate(sx, sy, sz, 0.0, 0.0, 0.0);
    }

    public static DoubleMatrix4 scale(double s)
    {
        return scale(s, s, s);
    }

    public static DoubleMatrix4 translate(double tx, double ty, double tz)
    {
        return scaleAndTranslate(1.0, 1.0, 1.0, tx, ty, tz);
    }

    public static DoubleMatrix4 translate(DoubleVector3 t)
    {
        return scaleAndTranslate(1.0, 1.0, 1.0, t.x, t.y, t.z);
    }

    public static DoubleMatrix4 ortho(double left, double right, double bottom, double top, double near, double far)
    {
        return new DoubleMatrix4(
            2 / (right - left),    0.0f,                0.0f,                    (right + left) / (left - right),
            0.0f,                2 / (top - bottom),    0.0f,                    (top + bottom) / (bottom - top),
            0.0f,                0.0f,                2.0f / (near - far),    (far + near) / (near - far),
            0.0f,                0.0f,                0.0f,                    1.0f
        );
    }

    public static DoubleMatrix4 ortho(double left, double right, double bottom, double top)
    {
        return ortho(left, right, bottom, top, -1.0f, 1.0f);
    }

    public static DoubleMatrix4 frustum(double left, double right, double bottom, double top, double near, double far)
    {
        return new DoubleMatrix4(
            2 * near / (right - left),    0.0f,                        (right + left) / (right - left),    0.0f,
            0.0f,                        2 * near / (top - bottom),    (top + bottom) / (top - bottom),    0.0f,
            0.0f,                        0.0f,                        (far + near) / (near - far),        2.0f * far * near / (near - far),
            0.0f,                        0.0f,                        -1.0f,                                0.0f
        );
    }

    public static DoubleMatrix4 perspective(double fovy, double aspect, double near, double far)
    {
        double f = 1.0f / Math.tan(fovy / 2);
        return new DoubleMatrix4(
            f / aspect,    0.0f,    0.0f,                            0.0f,
            0.0f,        f,        0.0f,                            0.0f,
            0.0f,        0.0f,    (far + near) / (near - far),    2.0f * far * near / (near - far),
            0.0f,        0.0f,    -1.0f,                            0.0f
        );
    }

    public static DoubleMatrix4 lookAt(
            DoubleVector3 eye,
            DoubleVector3 center,
            DoubleVector3 up)
    {
        DoubleVector3 f = center.minus(eye).normalized();
        DoubleVector3 upNormalized = up.normalized();
        DoubleVector3 s = f.cross(upNormalized).normalized();
        DoubleVector3 u = s.cross(f).normalized();

        return new DoubleMatrix4(
            s.x,     s.y,     s.z,     0.0f,
            u.x,     u.y,     u.z,     0.0f,
            -f.x,    -f.y,    -f.z,    0.0f,
            0.0f,    0.0f,    0.0f,    1.0f
        ).times(translate(-eye.x, -eye.y, -eye.z));
    }

    public static DoubleMatrix4 lookAt(
        double eyeX, double eyeY, double eyeZ,
        double centerX, double centerY, double centerZ,
        double upX, double upY, double upZ)
    {
        return lookAt(
            new DoubleVector3(eyeX, eyeY, eyeZ),
            new DoubleVector3(centerX, centerY, centerZ),
            new DoubleVector3(upX, upY, upZ)
        );
    }

    public static DoubleMatrix4 rotateX(double radians)
    {
        return DoubleMatrix3.rotateX(radians).asMatrix4();
    }

    public static DoubleMatrix4 rotateY(double radians)
    {
        return DoubleMatrix3.rotateY(radians).asMatrix4();
    }

    public static DoubleMatrix4 rotateZ(double radians)
    {
        return DoubleMatrix3.rotateZ(radians).asMatrix4();
    }

    public static DoubleMatrix4 rotateAxis(DoubleVector3 axis, double radians)
    {
        return DoubleMatrix3.rotateAxis(axis, radians).asMatrix4();
    }

    public static DoubleMatrix4 fromQuaternion(double x, double y, double z, double w)
    {
        return DoubleMatrix3.fromQuaternion(x, y, z, w).asMatrix4();
    }


    public DoubleMatrix3 getUpperLeft3x3()
    {
        return DoubleMatrix3.fromRows(
                new DoubleVector3(this.get(0,0), this.get(0,1),    this.get(0,2)),
                new DoubleVector3(this.get(1,0), this.get(1,1),    this.get(1,2)),
                new DoubleVector3(this.get(2,0), this.get(2,1),    this.get(2,2)) );
    }

    public DoubleMatrix4 plus(DoubleMatrix4 other)
    {
        return new DoubleMatrix4(
            this.m[0][0] + other.m[0][0],    this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2], this.m[0][3] + other.m[0][3],
            this.m[1][0] + other.m[1][0],    this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2], this.m[1][3] + other.m[1][3],
            this.m[2][0] + other.m[2][0],    this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2], this.m[2][3] + other.m[2][3],
            this.m[3][0] + other.m[3][0],    this.m[3][1] + other.m[3][1], this.m[3][2] + other.m[3][2], this.m[3][3] + other.m[3][3]
        );
    }

    public DoubleMatrix4 minus(DoubleMatrix4 other)
    {
        return new DoubleMatrix4(
            this.m[0][0] - other.m[0][0],    this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2], this.m[0][3] - other.m[0][3],
            this.m[1][0] - other.m[1][0],    this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2], this.m[1][3] - other.m[1][3],
            this.m[2][0] - other.m[2][0],    this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2], this.m[2][3] - other.m[2][3],
            this.m[3][0] - other.m[3][0],    this.m[3][1] - other.m[3][1], this.m[3][2] - other.m[3][2], this.m[3][3] - other.m[3][3]
        );
    }

    public DoubleMatrix4 times(DoubleMatrix4 other)
    {
        return new DoubleMatrix4(
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

    public DoubleVector4 times(DoubleVector4 vector)
    {
        return new DoubleVector4(
            this.m[0][0] * vector.x + this.m[0][1] * vector.y + this.m[0][2] * vector.z + this.m[0][3] * vector.w,
            this.m[1][0] * vector.x + this.m[1][1] * vector.y + this.m[1][2] * vector.z + this.m[1][3] * vector.w,
            this.m[2][0] * vector.x + this.m[2][1] * vector.y + this.m[2][2] * vector.z + this.m[2][3] * vector.w,
            this.m[3][0] * vector.x + this.m[3][1] * vector.y + this.m[3][2] * vector.z + this.m[3][3] * vector.w
        );
    }

    public DoubleMatrix4 negate()
    {
        return new DoubleMatrix4(
            -this.m[0][0], -this.m[0][1], -this.m[0][2], -this.m[0][3],
            -this.m[1][0], -this.m[1][1], -this.m[1][2], -this.m[1][3],
            -this.m[2][0], -this.m[2][1], -this.m[2][2], -this.m[2][3],
            -this.m[3][0], -this.m[3][1], -this.m[3][2], -this.m[3][3]
        );
    }

    public DoubleMatrix4 transpose()
    {
        return new DoubleMatrix4(
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
    public DoubleMatrix4 quickInverse(double tolerance)
    {
        DoubleMatrix3 rotationScale = this.getUpperLeft3x3();
        double scaleSquared = Math.pow(rotationScale.determinant(), 2.0 / 3.0);

        DoubleMatrix4 invCandidate = rotationScale.transpose().times(1.0 / scaleSquared).asMatrix4()
                .times(translate(this.getColumn(3).getXYZ().negated()));

        DoubleMatrix4 identityCandidate = this.times(invCandidate);

        double translationScale = this.getColumn(3).getXYZ().length();

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

    public double get(int row, int col)
    {
        return this.m[row][col];
    }

    public DoubleVector4 getRow(int row)
    {
        return new DoubleVector4(this.m[row][0], this.m[row][1], this.m[row][2], this.m[row][3]);
    }

    public DoubleVector4 getColumn(int col)
    {
        return new DoubleVector4(this.m[0][col], this.m[1][col], this.m[2][col], this.m[3][col]);
    }

    public DoubleBuffer asFloatBuffer()
    {
        return this.buffer.asReadOnlyBuffer();
    }
}
