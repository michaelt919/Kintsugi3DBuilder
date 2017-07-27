package tetzlaff.gl.nativebuffer;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class NativeUnsignedIntVectorBuffer implements NativeVectorBuffer
{
	private ByteBuffer buffer;
	
	final int dimensions;
	final int count;
	
	NativeUnsignedIntVectorBuffer(int dimensions, int count)
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
	
	NativeUnsignedIntVectorBuffer(int dimensions, int count, ByteBuffer buffer)
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
					"D vertices with 32-bit elements requires a buffer with a capacity of at least " + dimensions * count * 4 + ".");
		}
		if (buffer.order() != ByteOrder.nativeOrder())
		{
			throw new IllegalArgumentException("Buffers used by OpenGL must be in native byte order.");
		}
		
		this.dimensions = dimensions;
		this.count = count;
		this.buffer = buffer;
	}
	
	NativeUnsignedIntVectorBuffer(int dimensions, int count, byte[] buffer)
	{
		this(dimensions, count);
		this.buffer.put(buffer);
		this.buffer.flip();
	}
	
	public NativeUnsignedIntVectorBuffer(int dimensions, int count, int[] buffer)
	{
		this(dimensions, count);
		this.buffer.asIntBuffer().put(buffer);
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
	public Long get(int index, int dimension)
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
		return 0x00000000FFFFFFFFL & this.buffer.asIntBuffer().get(index * this.dimensions + dimension);
	}
	
	public void set(int index, int dimension, long value)
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
		this.buffer.asIntBuffer().put(index * this.dimensions + dimension, (int)value);
	}
	
	@Override
	public void set(int index, int dimension, Number value)
	{
		this.set(index, dimension, value.longValue());
	}
	
	@Override
	public ByteBuffer getBuffer()
	{
		return buffer;
	}

	@Override
	public NativeDataType getDataType() 
	{
		return NativeDataType.UNSIGNED_INT;
	}

	@Override
	public int getElementSizeInBytes() 
	{
		return 4;
	}
}
