package tetzlaff.gl.helpers;

public class Vector3 
{
	public final float x;
	public final float y;
	public final float z;

	public Vector3(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3 plus(Vector3 other)
	{
		return new Vector3(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z
		);
	}
	
	public Vector3 minus(Vector3 other)
	{
		return new Vector3(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z
		);
	}
	
	public Vector3 negated()
	{
		return new Vector3(-this.x, -this.y, -this.z);
	}
	
	public Vector3 times(float s)
	{
		return new Vector3(s*this.x, s*this.y, s*this.z);
	}
	
	public Vector3 dividedBy(float s)
	{
		return new Vector3(this.x/s, this.y/s, this.z/s);
	}
	
	public float dot(Vector3 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}
	
	public Vector3 cross(Vector3 other)
	{
		return new Vector3(
			this.y * other.z - this.z * other.y,
			this.z * other.x - this.x * other.z,
			this.x * other.y - this.y * other.x
		);
	}
	
	public float length()
	{
		return (float)Math.sqrt(this.dot(this));
	}
	
	public float distance(Vector3 other)
	{
		return this.minus(other).length();
	}
	
	public Vector3 normalized()
	{
		return this.times(1.0f / this.length());
	}
}
