package tetzlaff.gl.helpers;

public class Matrix3 
{
	private float[] m;
	
	public Matrix3(
		float m11, float m12, float m13,
		float m21, float m22, float m23,
		float m31, float m32, float m33)
    {
        m = new float[16];
        m[0] = m11;
        m[1] = m21;
        m[2] = m31;
        m[3] = m12;
        m[4] = m22;
        m[5] = m32;
        m[6] = m13;
        m[7] = m23;
        m[8] = m33;
    }

	public Matrix3(float sx, float sy, float sz) 
	{
		this(	sx, 	0.0f, 	0.0f,
				0.0f,	sy,		0.0f,
				0.0f,	0.0f, 	sz		);
	}
	
	public Matrix3(float s)
	{
		this(s, s, s);
	}

	public Matrix3() 
	{
		this(1.0f);
	}
	
	public static Matrix3 scale(float sx, float sy, float sz)
	{
		return new Matrix3(sx, sy, sz);
	}
	
	public static Matrix3 scale(float s)
	{
		return new Matrix3(s);
	}
	
	public static Matrix3 identity()
	{
		return new Matrix3();
	}
}
