package tetzlaff.gl.vecmath;

import java.util.function.DoubleUnaryOperator;

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

    public static final DoubleVector4 ZERO_DIRECTION = DoubleVector3.ZERO.asDirection();
    public static final DoubleVector4 ZERO_POSITION = DoubleVector3.ZERO.asPosition();

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

    public DoubleVector4 (double value)
    {
        this(value, value, value, value);
    }

    public DoubleVector2 getXY()
    {
        return new DoubleVector2(this.x, this.y);
    }

    public DoubleVector3 getXYZ()
    {
        return new DoubleVector3(this.x, this.y, this.z);
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
            new DoubleVector4(this.x * other.x, this.y * other.x, this.z * other.x, this.w * other.x),
            new DoubleVector4(this.x * other.y, this.y * other.y, this.z * other.y, this.w * other.y),
            new DoubleVector4(this.x * other.z, this.y * other.z, this.z * other.z, this.w * other.z),
            new DoubleVector4(this.x * other.w, this.y * other.w, this.z * other.w, this.w * other.w)
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

    public DoubleVector4 applyOperator(DoubleUnaryOperator operator)
    {
        return new DoubleVector4(operator.applyAsDouble(x), operator.applyAsDouble(y), operator.applyAsDouble(z), operator.applyAsDouble(w));
    }
}
