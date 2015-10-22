package tetzlaff.gl.helpers;

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
	/**
	 * The first dimension
	 */
	public final double x;
	
	/**
	 * The second dimension
	 */
	public final double y;
	
	/**
	 * The third dimension
	 */
	public final double z;
	
	/**
	 * The fourth dimension (or heterogeneous coordinate)
	 */
	public final double w;

	/**
	 * Construct a vector in four dimensions with the given values.
	 * @param x Value of the first dimension.
	 * @param y Value of the second dimension.
	 * @param z Value of the third dimension.
	 * @param w Value of the fourth dimension (or heterogeneous coordinate).
	 */
	public DoubleVector4(double x, double y, double z, double w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	/**
	 * Construct a vector in four dimensions from the given 2D vector and two scalar
	 * values for the missing dimensions.
	 * @param v2 The 2D vector from which the x and y values are copied.
	 * @param z Value of the third dimension.
	 * @param w Value of the fourth dimension (or heterogeneous coordinate).
	 */
	public DoubleVector4(DoubleVector2 v2, double z, double w)
	{
		this(v2.x, v2.y, z, w);
	}
	
	/**
	 * Construct a vector in four dimensions from the given 3D vector and a scalar
	 * value for the missing dimension.
	 * @param v3 The 3D vector from which the x, y and z values are copied.
	 * @param w Value of the fourth dimension (or heterogeneous coordinate).
	 */
	public DoubleVector4(DoubleVector3 v3, double w)
	{
		this(v3.x, v3.y, v3.z, w);
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public DoubleVector4 plus(DoubleVector4 other)
	{
		return new DoubleVector4(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z,
			this.w + other.w
		);
	}
	
	/**
	 * Construct a new vector as the subtraction of the given parameter from this.
	 * @param other The vector to subtract.
	 * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
	 */
	public DoubleVector4 minus(DoubleVector4 other)
	{
		return new DoubleVector4(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z,
			this.w - other.w
		);
	}
	
	/**
	 * Construct a new vector that is the negation of this.
	 * @return A new vector with the values (-x, -y, -z, -w)
	 */
	public DoubleVector4 negated()
	{
		return new DoubleVector4(-this.x, -this.y, -this.z, -this.w);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scalar.
	 * @param s The scalar to multiply by.
	 * @return A new vector equal to (s*x, s*y, s*z, s*w)
	 */
	public DoubleVector4 times(double s)
	{
		return new DoubleVector4(s*this.x, s*this.y, s*this.z, s*this.w);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scalar.
	 * @param s The scalar to divide by.
	 * @return A new vector equal to (x/s, y/s, z/s, w/s)
	 */
	public DoubleVector4 dividedBy(double s)
	{
		return new DoubleVector4(this.x/s, this.y/s, this.z/s, this.w/s);
	}
	
	/**
	 * Compute the dot product (scalar product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scalar value equal to the sum of x1*x2, y1*y2, z1*z2 and w1*w2.
	 */
	public double dot(DoubleVector4 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
	}
	
	/**
	 * Compute a scalar value representing the length/magnitude of this vector.
	 * @return A scalar value equal to square root of the sum of squares of the components.
	 */
	public double length()
	{
		return Math.sqrt(this.dot(this));
	}
	
	/**
	 * Calculate the distance between this and another given vector.
	 * @param other The vector to compute the distance between.
	 * @return A scalar value equal to the distance from the other vector.
	 */
	public double distance(DoubleVector4 other)
	{
		return this.minus(other).length();
	}
	
	/**
	 * Create a new vector with the same direction as this one but with unit
	 * magnitude (a length of 1.0).  
	 * Attempting to normalize a zero-length vector will result in NaN values.
	 * @return A new vector equal to this vector divided by it's length.
	 */
	public DoubleVector4 normalized()
	{
		return this.times(1.0 / this.length());
	}
}
