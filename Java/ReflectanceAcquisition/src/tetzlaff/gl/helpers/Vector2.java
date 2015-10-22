package tetzlaff.gl.helpers;

/**
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 32-bit floats.  This is an immutable object.
 *
 * @author Michael Tetzlaff
 */
public class Vector2 
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
	 * Construct a vector in two dimensions with the given values.
	 * @param x Value of the first dimension.
	 * @param y Value of the second dimension.
	 */
	public Vector2(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Construct a vector in two dimensions from the given 3D vector. The
	 * third dimension is discarded.
	 * @param v3 The 3D vector from which the x and y values are copied.
	 */
	public Vector2(Vector3 v3)
	{
		this(v3.x, v3.y);
	}
	
	/**
	 * Construct a vector in two dimensions from the given 4D vector. The
	 * third and fourth dimensions are discarded.
	 * @param v4 The 4D vector from which the x and y values are copied.
	 */
	public Vector2(Vector4 v4)
	{
		this(v4.x, v4.y);
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public Vector2 plus(Vector2 other)
	{
		return new Vector2(
			this.x + other.x,
			this.y + other.y
		);
	}
	
	/**
	 * Construct a new vector as the subtraction of the given parameter from this.
	 * @param other The vector to subtract.
	 * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
	 */
	public Vector2 minus(Vector2 other)
	{
		return new Vector2(
			this.x - other.x,
			this.y - other.y
		);
	}
	
	/**
	 * Construct a new vector that is the negation of this.
	 * @return A new vector with the values (-x, -y)
	 */
	public Vector2 negated()
	{
		return new Vector2(-this.x, -this.y);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scalar.
	 * @param s The scalar to multiply by.
	 * @return A new vector equal to (s*x, s*y)
	 */
	public Vector2 times(float s)
	{
		return new Vector2(s*this.x, s*this.y);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scalar.
	 * @param s The scalar to divide by.
	 * @return A new vector equal to (x/s, y/s)
	 */
	public Vector2 dividedBy(float s)
	{
		return new Vector2(this.x/s, this.y/s);
	}
	
	/**
	 * Compute the dot product (scalar product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scalar value equal to the sum of x1*x2 and y1*y2.
	 */
	public float dot(Vector2 other)
	{
		return this.x * other.x + this.y * other.y;
	}
	
	/**
	 * Compute a scalar value representing the length/magnitude of this vector.
	 * @return A scalar value equal to square root of the sum of squares of the components.
	 */
	public float length()
	{
		return (float)Math.sqrt(this.dot(this));
	}
	
	/**
	 * Calculate the distance between this and another given vector.
	 * @param other The vector to compute the distance between.
	 * @return A scalar value equal to the distance from the other vector.
	 */
	public float distance(Vector2 other)
	{
		return this.minus(other).length();
	}
	
	/**
	 * Create a new vector with the same direction as this one but with unit
	 * magnitude (a length of 1.0).  
	 * Attempting to normalize a zero-length vector will result in NaN values.
	 * @return A new vector equal to this vector divided by it's length.
	 */
	public Vector2 normalized()
	{
		return this.times(1.0f / this.length());
	}
}
