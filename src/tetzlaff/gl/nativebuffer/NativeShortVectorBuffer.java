package tetzlaff.gl.nativebuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import org.lwjgl.*;

class NativeShortVectorBuffer implements NativeVectorBuffer
{
    private ByteBuffer buffer;

    final int dimensions;
    final int count;

    NativeShortVectorBuffer(int dimensions, int count)
    {
        if (dimensions < 0)
        {
            throw new IllegalArgumentException("The number of vertex dimensions cannot be negative.");
        }
        if (count < 0)
        {
            throw new IllegalArgumentException("The number of vertices cannot be negative.");
        }

        this.dimensions = dimensions;
        this.count = count;
        this.buffer = BufferUtils.createByteBuffer(dimensions * count * 2);
    }

    NativeShortVectorBuffer(int dimensions, int count, ByteBuffer buffer)
    {
        if (dimensions < 0)
        {
            throw new IllegalArgumentException("The number of vertex dimensions cannot be negative.");
        }
        if (count < 0)
        {
            throw new IllegalArgumentException("The number of vertices cannot be negative.");
        }
        if (buffer.capacity() < dimensions * count * 2)
        {
            throw new IllegalArgumentException("Insufficient buffer size - a list of " + count + " " + dimensions +
                    "D 16-bit vertices requires a buffer with a capacity of at least " + dimensions * count * 2 + ".");
        }
        if (!Objects.equals(buffer.order(), ByteOrder.nativeOrder()))
        {
            throw new IllegalArgumentException("Buffers used by OpenGL must be in native byte order.");
        }

        this.dimensions = dimensions;
        this.count = count;
        this.buffer = buffer;
    }

    NativeShortVectorBuffer(int dimensions, int count, byte[] buffer)
    {
        this(dimensions, count);
        this.buffer.put(buffer);
        this.buffer.flip();
    }

    NativeShortVectorBuffer(int dimensions, int count, short[] buffer)
    {
        this(dimensions, count);
        this.buffer.asShortBuffer().put(buffer);
    }

    @Override
    public int getDimensions()
    {
        return dimensions;
    }

    @Override
    public int getCount()
    {
        return count;
    }

    @Override
    public Short get(int index, int dimension)
    {
        if (index < 0)
        {
            throw new IndexOutOfBoundsException("Index cannot be negative.");
        }
        if (index >= this.count)
        {
            throw new IndexOutOfBoundsException("Index (" + index + ") is greater than the size of the vertex list (" + this.count + ").");
        }
        if (dimension < 0)
        {
            throw new IndexOutOfBoundsException("Dimension cannot be negative.");
        }
        if (dimension >= this.dimensions)
        {
            throw new IndexOutOfBoundsException("Dimension (" + dimension + ") is greater than the dimensions of the vertex list (" + this.dimensions + ").");
        }
        return this.buffer.asShortBuffer().get(index * this.dimensions + dimension);
    }

    public void set(int index, int dimension, short value)
    {
        if (index < 0)
        {
            throw new IndexOutOfBoundsException("Index cannot be negative.");
        }
        if (index >= this.count)
        {
            throw new IndexOutOfBoundsException("Index (" + index + ") is greater than the size of the vertex list (" + this.count + ").");
        }
        if (dimension < 0)
        {
            throw new IndexOutOfBoundsException("Dimension cannot be negative.");
        }
        if (dimension >= this.dimensions)
        {
            throw new IndexOutOfBoundsException("Dimension (" + dimension + ") is greater than the dimensions of the vertex list (" + this.dimensions + ").");
        }
        this.buffer.asShortBuffer().put(index * this.dimensions + dimension, value);
    }

    @Override
    public void set(int index, int dimension, Number value)
    {
        this.set(index, dimension, value.shortValue());
    }

    @Override
    public ByteBuffer getBuffer()
    {
        return buffer;
    }

    @Override
    public NativeDataType getDataType()
    {
        return NativeDataType.SHORT;
    }

    @Override
    public int getElementSizeInBytes()
    {
        return 2;
    }
}
