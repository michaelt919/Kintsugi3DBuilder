package tetzlaff.gl.helpers;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class Matrix4 
{
	private float[][] m;
	private FloatBuffer buffer;
	
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
	
	public Matrix4(Matrix3 m3, float tx, float ty, float tz)
	{
		this(	m3.get(0,0),	m3.get(0,1),	m3.get(0,2),	tx,
				m3.get(1,0),	m3.get(1,1),	m3.get(1,2),	ty,
				m3.get(2,0),	m3.get(2,1),	m3.get(2,2),	tz,
				0.0f,			0.0f,			0.0f,			1.0f	);
	}
	
	public Matrix4(Matrix3 m3)
	{
		this(m3, 0.0f, 0.0f, 0.0f);
	}

	public Matrix4(float sx, float sy, float sz, 
			float tx, float ty, float tz) 
	{
		this(	sx, 	0.0f, 	0.0f, 	tx,
				0.0f,	sy,		0.0f,	ty,
				0.0f,	0.0f, 	sz,		tz,
				0.0f,	0.0f,	0.0f,	1.0f	);
	}

	public Matrix4(float sx, float sy, float sz) 
	{
		this(sx, sy, sz, 0.0f, 0.0f, 0.0f);
	}
	
	public Matrix4(float s)
	{
		this(s, s, s);
	}

	public Matrix4() 
	{
		this(1.0f);
	}
	
	public static Matrix4 scale(float sx, float sy, float sz)
	{
		return new Matrix4(sx, sy, sz);
	}
	
	public static Matrix4 scale(float s)
	{
		return new Matrix4(s);
	}
	
	public static Matrix4 identity()
	{
		return new Matrix4();
	}
	
	public static Matrix4 translate(float tx, float ty, float tz)
	{
		return new Matrix4(1.0f, 1.0f, 1.0f, tx, ty, tz);
	}
	
	public static Matrix4 translate(Vector3 t)
	{
		return new Matrix4(1.0f, 1.0f, 1.0f, t.x, t.y, t.z);
	}
	
	public static Matrix4 ortho(float left, float right, float bottom, float top, float near, float far)
	{
		return new Matrix4(
			2 / (right - left),	0.0f,				0.0f,					(right + left) / (left - right),
			0.0f,				2 / (top - bottom),	0.0f,					(top + bottom) / (bottom - top),
			0.0f,				0.0f,				2.0f / (near - far),	(far + near) / (near - far),
			0.0f,				0.0f,				0.0f,					1.0f
		);
	}
	
	public static Matrix4 ortho(float left, float right, float bottom, float top)
	{
		return Matrix4.ortho(left, right, bottom, top, -1.0f, 1.0f);
	}
	
	public static Matrix4 frustum(float left, float right, float bottom, float top, float near, float far)
	{
		return new Matrix4(
			2 * near / (right - left),	0.0f,						(right + left) / (right - left),	0.0f,
			0.0f,						2 * near / (top - bottom),	(top + bottom) / (top - bottom),	0.0f,
			0.0f,						0.0f,						(far + near) / (near - far),		2.0f * far * near / (near - far),
			0.0f,						0.0f,						-1.0f,								0.0f
		);
	}
	
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
	
	public static Matrix4 lookAt(
		float eyeX, float eyeY, float eyeZ,
		float centerX, float centerY, float centerZ,
		float upX, float upY, float upZ)
	{
		return Matrix4.lookAt(
			new Vector3(eyeX, eyeY, eyeZ),
			new Vector3(centerX, centerY, centerZ),
			new Vector3(upX, upY, upZ)
		);
	}
	
	public static Matrix4 rotateX(double radians)
	{
		return new Matrix4(Matrix3.rotateX(radians));
	}
	
	public static Matrix4 rotateY(double radians)
	{
		return new Matrix4(Matrix3.rotateY(radians));
	}
	
	public static Matrix4 rotateZ(double radians)
	{
		return new Matrix4(Matrix3.rotateZ(radians));
	}
	
	public static Matrix4 rotateAxis(Vector3 axis, double radians)
	{
		return new Matrix4(Matrix3.rotateAxis(axis, radians));
	}
	
	public static Matrix4 fromQuaternion(float x, float y, float z, float w)
	{
		return new Matrix4(Matrix3.fromQuaternion(x, y, z, w));
	}
	
	public Matrix4 plus(Matrix4 other)
	{
		return new Matrix4(
			this.m[0][0] + other.m[0][0],	this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2], this.m[0][3] + other.m[0][3],
			this.m[1][0] + other.m[1][0],	this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2], this.m[1][3] + other.m[1][3],
			this.m[2][0] + other.m[2][0],	this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2], this.m[2][3] + other.m[2][3],
			this.m[3][0] + other.m[3][0],	this.m[3][1] + other.m[3][1], this.m[3][2] + other.m[3][2], this.m[3][3] + other.m[3][3]
		);
	}
	
	public Matrix4 minus(Matrix4 other)
	{
		return new Matrix4(
			this.m[0][0] - other.m[0][0],	this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2], this.m[0][3] - other.m[0][3],
			this.m[1][0] - other.m[1][0],	this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2], this.m[1][3] - other.m[1][3],
			this.m[2][0] - other.m[2][0],	this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2], this.m[2][3] - other.m[2][3],
			this.m[3][0] - other.m[3][0],	this.m[3][1] - other.m[3][1], this.m[3][2] - other.m[3][2], this.m[3][3] - other.m[3][3]
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
		Matrix3 rotationScale = new Matrix3(this);
		float scale = rotationScale.determinant();
		
		Matrix4 invCandidate = new Matrix4(rotationScale.transpose().times(1.0f / (scale * scale))).times(Matrix4.translate(new Vector3(this.getColumn(3)).negated()));
		
		Matrix4 identityCandidate = this.times(invCandidate);
		
		if (Math.abs(identityCandidate.get(0, 0) - 1.0f) > tolerance ||
			Math.abs(identityCandidate.get(1, 1) - 1.0f) > tolerance ||
			Math.abs(identityCandidate.get(2, 2) - 1.0f) > tolerance ||
			Math.abs(identityCandidate.get(3, 3) - 1.0f) > tolerance ||
			Math.abs(identityCandidate.get(0, 1)) > tolerance ||
			Math.abs(identityCandidate.get(0, 2)) > tolerance ||
			Math.abs(identityCandidate.get(0, 3)) > tolerance ||
			Math.abs(identityCandidate.get(1, 0)) > tolerance ||
			Math.abs(identityCandidate.get(1, 2)) > tolerance ||
			Math.abs(identityCandidate.get(1, 3)) > tolerance ||
			Math.abs(identityCandidate.get(2, 0)) > tolerance ||
			Math.abs(identityCandidate.get(2, 1)) > tolerance ||
			Math.abs(identityCandidate.get(2, 3)) > tolerance ||
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
}
