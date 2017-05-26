package tetzlaff.gl.vecmath;

/**
 * A vector of four dimensions (for linear algebra calculations) backed by 
 * 32-bit integers.
 * All arithmetic other than length() or distance() will be integer arithmetic, including division.  
 * This is an immutable object.
 * 
 * @see Vector4
 * @author Michael Tetzlaff
 */
public class IntVector4 
{
	public final int x;
	public final int y;
	public final int z;
	public final int w;

	IntVector4(int x, int y, int z, int w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public static IntVector4 fromScalars(int x, int y, int z, int w)
	{
		return new IntVector4(x, y, z, w);
	}
	
	public static IntVector4 fromScalar(int value)
	{
		return new IntVector4(value, value, value, value);
	}
	
	public static IntVector4 fromVector2(IntVector2 v2, int z, int w)
	{
		return new IntVector4(v2.x, v2.y, z, w);
	}
	
	public static IntVector4 fromVector3(IntVector3 v3, int w)
	{
		return new IntVector4(v3.x, v3.y, v3.z, w);
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
