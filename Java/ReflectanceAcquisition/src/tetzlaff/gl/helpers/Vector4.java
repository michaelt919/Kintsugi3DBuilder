package tetzlaff.gl.helpers;

public class Vector4 
{
	public final float x;
	public final float y;
	public final float z;
	public final float w;

	public Vector4(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Vector4(Vector2 v2, float z, float w)
	{
		this(v2.x, v2.y, z, w);
	}
	
	public Vector4(Vector3 v3, float w)
	{
		this(v3.x, v3.y, v3.z, w);
	}
	
	public Vector4 plus(Vector4 other)
	{
		return new Vector4(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z,
			this.w + other.w
		);
	}
	
	public Vector4 minus(Vector4 other)
	{
		return new Vector4(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z,
			this.w - other.w
		);
	}
	
	public Vector4 negated()
	{
		return new Vector4(-this.x, -this.y, -this.z, -this.w);
	}
	
	public Vector4 times(float s)
	{
		return new Vector4(s*this.x, s*this.y, s*this.z, s*this.w);
	}
	
	public Vector4 dividedBy(float s)
	{
		return new Vector4(this.x/s, this.y/s, this.z/s, this.w/s);
	}
	
	public float dot(Vector4 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
	}
	
	public float length()
	{
		return (float)Math.sqrt(this.dot(this));
	}
	
	public float distance(Vector4 other)
	{
		return this.minus(other).length();
	}
	
	public Vector4 normalized()
	{
		return this.times(1.0f / this.length());
	}
}
