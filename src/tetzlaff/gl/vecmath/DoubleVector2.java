package tetzlaff.gl.vecmath;

/**
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  This is an immutable object.
 * 
 * @see Vector2
 * @author Michael Tetzlaff
 */
public class DoubleVector2 
{
	public final double x;
	public final double y;

	private DoubleVector2(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public static DoubleVector2 fromScalars(double x, double y)
	{
		return new DoubleVector2(x, y);
	}
	
	/**
	 * Construct a vector in two dimensions with the given values.
	 * @param value Value of both dimensions.
	 */
	public static DoubleVector2 fromScalar(double value)
	{
		return new DoubleVector2(value, value);
	}
	
	public static DoubleVector2 takeXY(DoubleVector3 v3)
	{
		return new DoubleVector2(v3.x, v3.y);
	}
	
	public static DoubleVector2 takeXY(DoubleVector4 v4)
	{
		return new DoubleVector2(v4.x, v4.y);
	}
	
	public DoubleVector2 plus(DoubleVector2 other)
	{
		return new DoubleVector2(
			this.x + other.x,
			this.y + other.y
		);
	}
	
	public DoubleVector2 minus(DoubleVector2 other)
	{
		return new DoubleVector2(
			this.x - other.x,
			this.y - other.y
		);
	}
	
	public DoubleVector2 negated()
	{
		return new DoubleVector2(-this.x, -this.y);
	}
	
	public DoubleVector2 times(double s)
	{
		return new DoubleVector2(s*this.x, s*this.y);
	}
	
	public DoubleVector2 dividedBy(double s)
	{
		return new DoubleVector2(this.x/s, this.y/s);
	}
	
	public double dot(DoubleVector2 other)
	{
		return this.x * other.x + this.y * other.y;
	}
	
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	public double distance(DoubleVector2 other)
	{
		return this.minus(other).length();
	}
	
	public DoubleVector2 normalized()
	{
		return this.times(1.0 / this.length());
	}
}
