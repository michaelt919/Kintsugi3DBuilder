package tetzlaff.gl.helpers;

public class Matrix3 
{
	private float[][] m;
	
	public Matrix3(
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
	
	public static Matrix3 rotateX(double radians)
	{
		float sinTheta = (float)Math.sin(radians);
		float cosTheta = (float)Math.cos(radians);
		return new Matrix3(
			1.0f,	0.0f, 		0.0f,
			0.0f,	cosTheta,	-sinTheta,
			0.0f,	sinTheta,	cosTheta
		);
	}
	
	public static Matrix3 rotateY(double radians)
	{
		float sinTheta = (float)Math.sin(radians);
		float cosTheta = (float)Math.cos(radians);
		return new Matrix3(
			cosTheta,	0.0f, 	sinTheta,
			0.0f,		1.0f,	0.0f,
			-sinTheta,	0.0f,	cosTheta
		);
	}
	
	public static Matrix3 rotateZ(double radians)
	{
		float sinTheta = (float)Math.sin(radians);
		float cosTheta = (float)Math.cos(radians);
		return new Matrix3(
			cosTheta,	-sinTheta,	0.0f,
			sinTheta,	cosTheta,	0.0f,
			0.0f,		0.0f,		1.0f
		);
	}
	
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
	
	public Matrix3 plus(Matrix3 other)
	{
		return new Matrix3(
			this.m[0][0] + other.m[0][0],	this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2],
			this.m[1][0] + other.m[1][0],	this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2],
			this.m[2][0] + other.m[2][0],	this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2]
		);
	}
	
	public Matrix3 minus(Matrix3 other)
	{
		return new Matrix3(
			this.m[0][0] - other.m[0][0],	this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2],
			this.m[1][0] - other.m[1][0],	this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2],
			this.m[2][0] - other.m[2][0],	this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2]
		);
	}
	
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
	
	public Matrix3 negate()
	{
		return new Matrix3(
			-this.m[0][0], -this.m[0][1], -this.m[0][2],
			-this.m[1][0], -this.m[1][1], -this.m[1][2],
			-this.m[2][0], -this.m[2][1], -this.m[2][2]
		);
	}
	
	public Matrix3 transpose()
	{
		return new Matrix3(
			this.m[0][0], this.m[1][0], this.m[2][0],
			this.m[0][1], this.m[1][1], this.m[2][1],
			this.m[0][2], this.m[1][2], this.m[2][2]
		);
	}
	
	public float get(int row, int col)
	{
		return this.m[row][col];
	}
}
