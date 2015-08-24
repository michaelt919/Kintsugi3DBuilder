package tetzlaff.gl.helpers;

/**
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 * @see Vector3
 */
public class DoubleVector3 
{
	public final double x;
	public final double y;
	public final double z;

	public DoubleVector3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public DoubleVector3(DoubleVector2 v2, double z)
	{
		this(v2.x, v2.y, z);
	}
	
	public DoubleVector3(DoubleVector4 v4)
	{
		this(v4.x, v4.y, v4.z);
	}
	
	public DoubleVector3 plus(DoubleVector3 other)
	{
		return new DoubleVector3(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z
		);
	}
	
	public DoubleVector3 minus(DoubleVector3 other)
	{
		return new DoubleVector3(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z
		);
	}
	
	public DoubleVector3 negated()
	{
		return new DoubleVector3(-this.x, -this.y, -this.z);
	}
	
	public DoubleVector3 times(double s)
	{
		return new DoubleVector3(s*this.x, s*this.y, s*this.z);
	}
	
	public DoubleVector3 dividedBy(double s)
	{
		return new DoubleVector3(this.x/s, this.y/s, this.z/s);
	}
	
	public double dot(DoubleVector3 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}
	
	public DoubleVector3 cross(DoubleVector3 other)
	{
		return new DoubleVector3(
			this.y * other.z - this.z * other.y,
			this.z * other.x - this.x * other.z,
			this.x * other.y - this.y * other.x
		);
	}
	
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	public double distance(DoubleVector3 other)
	{
		return this.minus(other).length();
	}
	
	public DoubleVector3 normalized()
	{
		return this.times(1.0 / this.length());
	}
}
