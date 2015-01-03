package tetzlaff.gl.helpers;

public class Vector2 
{
	public final float x;
	public final float y;

	public Vector2(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Vector2 plus(Vector2 other)
	{
		return new Vector2(
			this.x + other.x,
			this.y + other.y
		);
	}
	
	public Vector2 minus(Vector2 other)
	{
		return new Vector2(
			this.x - other.x,
			this.y - other.y
		);
	}
	
	public Vector2 negated()
	{
		return new Vector2(-this.x, -this.y);
	}
	
	public Vector2 times(float s)
	{
		return new Vector2(s*this.x, s*this.y);
	}
	
	public Vector2 dividedBy(float s)
	{
		return new Vector2(this.x/s, this.y/s);
	}
	
	public float dot(Vector2 other)
	{
		return this.x * other.x + this.y * other.y;
	}
	
	public float length()
	{
		return (float)Math.sqrt(this.dot(this));
	}
	
	public float distance(Vector2 other)
	{
		return this.minus(other).length();
	}
	
	public Vector2 normalized()
	{
		return this.times(1.0f / this.length());
	}
}
