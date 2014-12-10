package tetzlaff.gl.helpers;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class Matrix4 
{
	private float[] m;
	private FloatBuffer buffer;
	
	public Matrix4(
		float m11, float m12, float m13, float m14,
		float m21, float m22, float m23, float m24,
		float m31, float m32, float m33, float m34,
		float m41, float m42, float m43, float m44)
    {
        m = new float[16];
        m[0] = m11;
        m[1] = m21;
        m[2] = m31;
        m[3] = m41;
        m[4] = m12;
        m[5] = m22;
        m[6] = m32;
        m[7] = m42;
        m[8] = m13;
        m[9] = m23;
        m[10] = m33;
        m[11] = m43;
        m[12] = m14;
        m[13] = m24;
        m[14] = m34;
        m[15] = m44;
        
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
		return new Matrix4(0.0f, 0.0f, 0.0f, tx, ty, tz);
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
		float f = 1.0f / (float)Math.tan(fovy * 0.5f);
		return new Matrix4(
			f / aspect,	0.0f,	0.0f,							0.0f,
			0.0f,		f,		0.0f,							0.0f,
			0.0f,		0.0f,	(far + near) / (near - far),	2.0f * far * near / (near - far),
			0.0f,		0.0f,	-1.0f,							0.0f
		);
	}

	public FloatBuffer asFloatBuffer() 
	{
		return this.buffer.asReadOnlyBuffer();
	}
}
