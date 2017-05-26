package tetzlaff.gl.vecmath;

/**
 * @author Michael Tetzlaff
 * 
 * A vector of four dimensions (for linear algebra calculations) backed by 
 * 64-bit floats.  Useful for heterogeneous coordinate calculations. This
 * is an immutable object.
 * 
 * @see Vector4
 */
public class DoubleVector4 
{
	public final double x;
	public final double y;
	public final double z;
	public final double w;
	
	public static final DoubleVector4 ZERO_DIRECTION = fromVector3AsDirection(DoubleVector3.ZERO);
	public static final DoubleVector4 ZERO_POSITION = fromVector3AsPosition(DoubleVector3.ZERO);

	private DoubleVector4(double x, double y, double z, double w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public static DoubleVector4 fromScalar(double value)
	{
		return new DoubleVector4(value, value, value, value);
	}
	
	public static DoubleVector4 fromScalars(double x, double y, double z, double w)
	{
		return new DoubleVector4(x, y, z, w);
	}
	
	public static DoubleVector4 fromVector2(DoubleVector2 v2, double z, double w)
	{
		return new DoubleVector4(v2.x, v2.y, z, w);
	}
	
	public static DoubleVector4 fromVector3(DoubleVector3 v3, double w)
	{
		return new DoubleVector4(v3.x, v3.y, v3.z, w);
	}
	
	public static DoubleVector4 fromVector3AsDirection(DoubleVector3 v3)
	{
		return new DoubleVector4(v3.x, v3.y, v3.z, 0.0);
	}
	
	public static DoubleVector4 fromVector3AsPosition(DoubleVector3 v3)
	{
		return new DoubleVector4(v3.x, v3.y, v3.z, 1.0);
	}
	
	public DoubleVector4 plus(DoubleVector4 other)
	{
		return new DoubleVector4(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z,
			this.w + other.w
		);
	}
	
	public DoubleVector4 minus(DoubleVector4 other)
	{
		return new DoubleVector4(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z,
			this.w - other.w
		);
	}
	
	public DoubleVector4 negated()
	{
		return new DoubleVector4(-this.x, -this.y, -this.z, -this.w);
	}
	
	public DoubleVector4 times(double s)
	{
		return new DoubleVector4(s*this.x, s*this.y, s*this.z, s*this.w);
	}
	
	public DoubleVector4 dividedBy(double s)
	{
		return new DoubleVector4(this.x/s, this.y/s, this.z/s, this.w/s);
	}
	
	public double dot(DoubleVector4 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
	}
	
	/**
	 * Compute the outer product of this vector and another given vector.
	 * @param other The vector to use when computing the outer product.
	 * @return The matrix that is the outer product of the vectors.
	 */
	public DoubleMatrix4 outerProduct(DoubleVector4 other)
	{
		return DoubleMatrix4.fromColumns(
			DoubleVector4.fromScalars(this.x * other.x, this.y * other.x, this.z * other.x, this.w * other.x),
			DoubleVector4.fromScalars(this.x * other.y, this.y * other.y, this.z * other.y, this.w * other.y),
			DoubleVector4.fromScalars(this.x * other.z, this.y * other.z, this.z * other.z, this.w * other.z),
			DoubleVector4.fromScalars(this.x * other.w, this.y * other.w, this.z * other.w, this.w * other.w)
		);
	}
	
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	public double distance(DoubleVector4 other)
	{
		return this.minus(other).length();
	}
	
	public DoubleVector4 normalized()
	{
		return this.times(1.0 / this.length());
	}
}
