package tetzlaff.gl.vecmath;

/**
 * A vector of three dimensions (for linear algebra calculations) backed by 
 * 32-bit floats.  This is an immutable object.
 * 
 * @author Michael Tetzlaff
 */
public class Vector3 
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
	
	public static final Vector3 ZERO = fromScalar(0.0f);

	/**
	 * Construct a vector in three dimensions with the given values.
	 * @param x Value of the first dimension.
	 * @param y Value of the second dimension.
	 * @param z Value of the third dimension.
	 */
	private Vector3(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public static Vector3 fromScalars(float x, float y, float z)
	{
		return new Vector3(x, y, z);
	}
	
	/**
	 * Construct a vector in three dimensions with the given values.
	 * @param value Value of all three dimensions.
	 */
	public static Vector3 fromScalar(float value)
	{
		return new Vector3(value, value, value);
	}
	
	public static Vector3 fromVector2(Vector2 v2, float z)
	{
		return new Vector3(v2.x, v2.y, z);
	}
	
	public static Vector3 takeXYZ(Vector4 v4)
	{
		return new Vector3(v4.x, v4.y, v4.z);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Vector3)
		{
			Vector3 other = (Vector3)o;
			return other.x == this.x && other.y == this.y && other.z == this.z;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Construct a new vector as the sum of this one and the given parameter.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) sum of this and 'other'.
	 */
	public Vector3 plus(Vector3 other)
	{
		return new Vector3(
			this.x + other.x,
			this.y + other.y,
			this.z + other.z
		);
	}
	
	/**
	 * Construct a new vector as the subtraction of the given parameter from this.
	 * @param other The vector to add.
	 * @return A new vector that is the mathematical (componentwise) subtraction of 'other' from this.
	 */
	public Vector3 minus(Vector3 other)
	{
		return new Vector3(
			this.x - other.x,
			this.y - other.y,
			this.z - other.z
		);
	}
	
	/**
	 * Construct a new vector that is the negation of this.
	 * @return A new vector with the values (-x, -y, -z)
	 */
	public Vector3 negated()
	{
		return new Vector3(-this.x, -this.y, -this.z);
	}
	
	/**
	 * Construct a new vector that is the product of this and a given scaler.
	 * @param s The scaler to multiply by.
	 * @return A new vector equal to (s*x, s*y, s*z)
	 */
	public Vector3 times(float s)
	{
		return new Vector3(s*this.x, s*this.y, s*this.z);
	}
	
	/**
	 * Construct a new vector that is the quotient of this and a given scaler.
	 * @param s The scaler to divide by.
	 * @return A new vector equal to (x/s, y/s, z/s)
	 */
	public Vector3 dividedBy(float s)
	{
		return new Vector3(this.x/s, this.y/s, this.z/s);
	}
	
	/**
	 * Compute the dot product (scalar product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A scalar value equal to the sum of x1*x2, y1*y2 and z1*z2.
	 */
	public float dot(Vector3 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}
	
	/**
	 * Compute the outer product of this vector and another given vector.
	 * @param other The vector to use when computing the outer product.
	 * @return The matrix that is the outer product of the vectors.
	 */
	public Matrix3 outerProduct(Vector3 other)
	{
		return Matrix3.fromColumns(
			Vector3.fromScalars(this.x * other.x, this.y * other.x, this.z * other.x),
			Vector3.fromScalars(this.x * other.y, this.y * other.y, this.z * other.y),
			Vector3.fromScalars(this.x * other.z, this.y * other.z, this.z * other.z)
		);
	}
	
	/**
	 * Compute the cross product (vector product) of this vector and another given vector.
	 * @param other The vector to use when computing the dot product.
	 * @return A new 3d vector with the values (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2)
	 */
	public Vector3 cross(Vector3 other)
	{
		return new Vector3(
			this.y * other.z - this.z * other.y,
			this.z * other.x - this.x * other.z,
			this.x * other.y - this.y * other.x
		);
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
	public float distance(Vector3 other)
	{
		return this.minus(other).length();
	}
	
	/**
	 * Create a new vector with the same direction as this one but with unit
	 * magnitude (a length of 1.0).  CAUTION!  May cause divide by zero error.
	 * Do not attempt to normalize a zero-length vector.
	 * @return A new vector equal to this vector divided by it's length.
	 */
	public Vector3 normalized()
	{
		return this.times(1.0f / this.length());
	}
}
