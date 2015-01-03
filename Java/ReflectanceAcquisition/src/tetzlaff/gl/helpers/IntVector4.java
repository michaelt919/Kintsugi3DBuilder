package tetzlaff.gl.helpers;

public class IntVector4 
{
	public final int x;
	public final int y;
	public final int z;
	public final int w;

	public IntVector4(int x, int y, int z, int w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public IntVector4 plus(IntVector4 other)
	{
		return new IntVector4(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z,
			this.w + other.w
		);
	}
	
	public IntVector4 minus(IntVector4 other)
	{
		return new IntVector4(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z,
			this.w - other.w
		);
	}
	
	public IntVector4 negated()
	{
		return new IntVector4(-this.x, -this.y, -this.z, -this.w);
	}
	
	public IntVector4 times(int s)
	{
		return new IntVector4(s*this.x, s*this.y, s*this.z, s*this.w);
	}
	
	public IntVector4 dividedBy(int s)
	{
		return new IntVector4(this.x/s, this.y/s, this.z/s, this.w/s);
	}
	
	public int dot(IntVector4 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
	}
	
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	public double distance(IntVector4 other)
	{
		return this.minus(other).length();
	}
}
