/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.nativebuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import org.lwjgl.*;

class NativeFloatVectorBuffer implements NativeVectorBuffer
{
    private final ByteBuffer buffer;

    final int dimensions;
    final int count;

    NativeFloatVectorBuffer(int dimensions, int count)
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
        this.buffer = BufferUtils.createByteBuffer(dimensions * count * 4);
    }

    NativeFloatVectorBuffer(int dimensions, int count, ByteBuffer buffer)
    {
        if (dimensions < 0)
        {
            throw new IllegalArgumentException("The number of vertex dimensions cannot be negative.");
        }
        if (count < 0)
        {
            throw new IllegalArgumentException("The number of vertices cannot be negative.");
        }
        if (buffer.capacity() < dimensions * count * 4)
        {
            throw new IllegalArgumentException("Insufficient buffer size - a list of " + count + dimensions +
                    "D 32-bit vertices requires a buffer with a capacity of at least " + dimensions * count * 4 + '.');
        }
        if (!Objects.equals(buffer.order(), ByteOrder.nativeOrder()))
        {
            throw new IllegalArgumentException("Buffers used by OpenGL must be in native byte order.");
        }

        this.dimensions = dimensions;
        this.count = count;
        this.buffer = buffer;
    }

    NativeFloatVectorBuffer(int dimensions, int count, byte... buffer)
    {
        this(dimensions, count);
        this.buffer.put(buffer);
        this.buffer.flip();
    }

    NativeFloatVectorBuffer(int dimensions, int count, float... buffer)
    {
        this(dimensions, count);
        this.buffer.asFloatBuffer().put(buffer);
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
    public Float get(int index, int dimension)
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
        return this.buffer.asFloatBuffer().get(index * this.dimensions + dimension);
    }

    public void set(int index, int dimension, float value)
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
        this.buffer.asFloatBuffer().put(index * this.dimensions + dimension, value);
    }

    @Override
    public void set(int index, int dimension, Number value)
    {
        this.set(index, dimension, value.floatValue());
    }

    @Override
    public ByteBuffer getBuffer()
    {
        return buffer;
    }

    @Override
    public NativeDataType getDataType()
    {
        return NativeDataType.FLOAT;
    }
}
