package tetzlaff.gl.vecmath;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A vector of two dimensions (for linear algebra calculations) backed by 
 * 32-bit integers.
 * All arithmetic other than length() or distance() will be integer arithmetic, including division.  
 * This is an immutable object.
 * 
 * @see Vector2
 * @author Michael Tetzlaff
 */
public class IntVector2 implements Iterable<Integer>
{
    public final int x;
    public final int y;

    public IntVector2(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Construct a vector in two dimensions with the given values.
     * @param value Value of both dimensions.
     */
    public IntVector2(int value)
    {
        this(value, value);
    }

    public IntVector3 asVector3(int z)
    {
        return new IntVector3(this.x, this.y, z);
    }

    public IntVector4 asVector4(int z, int w)
    {
        return new IntVector4(this.x, this.y, z, w);
    }

    public Vector2 asFloatingPoint()
    {
        return new Vector2(x, y);
    }

    public Vector2 asFloatingPointNormalized()
    {
        return new Vector2(x / 255.0f, y / 255.0f);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof IntVector2)
        {
            IntVector2 other = (IntVector2) obj;
            return other.x == this.x && other.y == this.y;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 + x) + y;
    }

    public IntVector2 plus(IntVector2 other)
    {
        return new IntVector2(
            this.x + other.x,
            this.y + other.y
        );
    }

    public IntVector2 minus(IntVector2 other)
    {
        return new IntVector2(
            this.x - other.x,
            this.y - other.y
        );
    }

    public IntVector2 negated()
    {
        return new IntVector2(-this.x, -this.y);
    }

    public IntVector2 times(int s)
    {
        return new IntVector2(s*this.x, s*this.y);
    }

    public IntVector2 dividedBy(int s)
    {
        return new IntVector2(this.x/s, this.y/s);
    }

    public int dot(IntVector2 other)
    {
        return this.x * other.x + this.y * other.y;
    }

    public double length()
    {
        return Math.sqrt(this.dot(this));
    }

    public double distance(IntVector2 other)
    {
        return this.minus(other).length();
    }

    @Override
    public Iterator<Integer> iterator()
    {
        return Arrays.asList(x, y).iterator();
    }
}
