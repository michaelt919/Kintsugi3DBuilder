package tetzlaff.gl.helpers;

/**
 * A vector of four dimensions (for linear algebra calculations) backed by 
 * 32-bit floats.  Useful for homogeneous coordinates in three dimensional
 * space.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 */
public class Vector4 
{
	/**
	 * The first dimension
	 */
	public final float x;
	/**
	 * The second dimension
	 */
	public final float y;
	/**
	 * The third dimension
	 */
	public final float z;
	/**
	 * The fourth dimension (or heterogeneous coordinate)
	 */
	public final float w;

	/**
	 * Construct a vector in four dimensions with the given values.
	 * @param x Value of the first dimension.
	 * @param y Value of the second dimension.
	 * @param z Value of the third dimension.
	 * @param w Value of the fourth dimension (or heterogeneous coordinate).
	 */
	public Vector4(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	/**
	 * Construct a vector in four dimensions from the given 2D vector and two scaler
	 * values for the missing dimensions.
	 * @param v2 The 2D vector from which the x and y values are copied.
	 * @param z Value of the third dimension.
	 * @param w Value of the fourth dimension (or heterogeneous coordinate).
	 */
	public Vector4(Vector2 v2, float z, float w)
	{
		this(v2.x, v2.y, z, w);
	}
	
	/**
	 * Construct a vector in four dimensions from the given 3D vector and a scaler
	 * value for the missing dimension.
	 * @param v3 The 3D vector from which the x, y and z values are copied.
	 * @param w Value of the fourth dimension (or heterogeneous coordinate).
	 */
	public Vector4(Vector3 v3, float w)
	{
		this(v3.x, v3.y, v3.z, w);
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public Vector4 plus(Vector4 other)
	{
		return new Vector4(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z,
			this.w + other.w
		);
	}
	
	/**
	 * Construct a new vector as the subtraction of the given parameter from this.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
	 */
	public Vector4 minus(Vector4 other)
	{
		return new Vector4(
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
	public Vector4 negated()
	{
		return new Vector4(-this.x, -this.y, -this.z, -this.w);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scaler.
	 * @param s The scaler to multiply by.
	 * @return A new vector equal to (s*x, s*y, s*z, s*w)
	 */
	public Vector4 times(float s)
	{
		return new Vector4(s*this.x, s*this.y, s*this.z, s*this.w);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scaler.
	 * @param s The scaler to divide by.
	 * @return A new vector equal to (x/s, y/s, z/s, w/s)
	 */
	public Vector4 dividedBy(float s)
	{
		return new Vector4(this.x/s, this.y/s, this.z/s, this.w/s);
	}
	
	/**
	 * Compute the dot product (scaler product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scaler value equal to the sum of x1*x2, y1*y2, z1*z2 and w1*w2.
	 */
	public float dot(Vector4 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
	}
	
	/**
	 * Compute a scaler value representing the length/magnitude of this vector.
	 * @return A scaler value equal to square root of the sum of squares of the components.
	 */
	public float length()
	{
		return (float)Math.sqrt(this.dot(this));
	}
	
	/**
	 * Calculate the distance between this and another given vector.
	 * @param other The vector to compute the distance between.
	 * @return A scaler value equal to the length of the different vector.
	 */
	public float distance(Vector4 other)
	{
		return this.minus(other).length();
	}
	
	/**
	 * Create a new vector with the same direction as this one but with unit
	 * magnitude (a length of 1.0).  CAUTION!  May cause divide by zero error.
	 * Do not attempt to normalize a zero-length vector.
	 * @return A new vector equal to this vector divided by it's length.
	 */
	public Vector4 normalized()
	{
		return this.times(1.0f / this.length());
	}
}
