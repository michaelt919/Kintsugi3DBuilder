/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl.helpers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;

/**
 * A data structure for representing a list of vertices to be packed in a buffer.
 * Each dimension of every vertex is to be represented as an 8-bit integer.
 * Every vertex in the list has the same number of dimensions, which is the number of elements associated with a single vertex.
 * @author Michael Tetzlaff
 *
 */
public class ByteVertexList
{
	/**
	 * The native buffer in which the vertices are stored.
	 */
	private ByteBuffer buffer;
	
	/**
	 * The number of dimensions in each vertex.
	 */
	public final int dimensions;
	
	/**
	 * The number of vertices in the list.
	 */
	public final int count;
	
	/**
	 * Creates a new vertex list with the specified number of dimensions and vertices.
	 * @param dimensions The number of dimensions in each vertex.
	 * @param count The number of vertices in the list.
	 */
	public ByteVertexList(int dimensions, int count)
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
		this.buffer = BufferUtils.createByteBuffer(dimensions * count);
	}
	
	/**
	 * Creates a new vertex list with the specified number of dimensions and vertices, using a pre-existing buffer as storage.
	 * Any data already in the buffer will persist.
	 * A runtime exception will be thrown if the buffer is not big enough.
	 * @param dimensions The number of dimensions in each vertex.
	 * @param count The number of vertices in the list.
	 * @param buffer The buffer to use as storage.
	 */
	public ByteVertexList(int dimensions, int count, ByteBuffer buffer)
	{
		if (dimensions < 0)
		{
			throw new IllegalArgumentException("The number of vertex dimensions cannot be negative.");
		}
		if (count < 0)
		{
			throw new IllegalArgumentException("The number of vertices cannot be negative.");
		}
		if (buffer.capacity() < dimensions * count)
		{
			throw new IllegalArgumentException("Insufficient buffer size - a list of " + count + dimensions +
					"D vertices requires a buffer with a capacity of at least " + dimensions * count + ".");
		}
		if (buffer.order() != ByteOrder.nativeOrder())
		{
			throw new IllegalArgumentException("Buffers used by OpenGL must be in native byte order.");
		}
		
		this.dimensions = dimensions;
		this.count = count;
		this.buffer = buffer;
	}
	
	/**
	 * Creates a new vertex list with the specified number of dimensions and vertices, and an array of initial values to write to the buffer.
	 * @param dimensions The number of dimensions in each vertex.
	 * @param count The number of vertices in the list.
	 * @param buffer The buffer to use to initialize the vertex list.
	 */
	public ByteVertexList(int dimensions, int count, byte[] buffer)
	{
		this(dimensions, count);
		this.buffer.put(buffer);
		this.buffer.flip();
	}
	
	/**
	 * Gets the value of a particular dimension of a particular vertex.
	 * @param index The index of the vertex to retrieve.
	 * @param dimension The dimension within the vertex to retrieve.
	 * @return The value of the dimension of the vertex.
	 */
	public byte get(int index, int dimension)
	{
		if (index < 0)
		{
			throw new IndexOutOfBoundsException("Index cannot be negative.");
		}
		if (index > this.count)
		{
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than the size of the vertex list (" + this.count + ").");
		}		
		if (dimension < 0)
		{
			throw new IndexOutOfBoundsException("Dimension cannot be negative.");
		}
		if (dimension > this.dimensions)
		{
			throw new IndexOutOfBoundsException("Dimension (" + dimension + ") is greater than the dimensions of the vertex list (" + this.dimensions + ").");
		}
		return this.buffer.get(index * this.dimensions + dimension);
	}
	
	/**
	 * Sets the value of a particular dimension of a particular vertex.
	 * @param index The index of the vertex to retrieve.
	 * @param dimension The dimension within the vertex to retrieve.
	 * @param value The value to set the dimension of the vertex to.
	 */
	public void set(int index, int dimension, byte value)
	{
		if (index < 0)
		{
			throw new IndexOutOfBoundsException("Index cannot be negative.");
		}
		if (index > this.count)
		{
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than the size of the vertex list (" + this.count + ").");
		}		
		if (dimension < 0)
		{
			throw new IndexOutOfBoundsException("Dimension cannot be negative.");
		}
		if (dimension > this.dimensions)
		{
			throw new IndexOutOfBoundsException("Dimension (" + dimension + ") is greater than the dimensions of the vertex list (" + this.dimensions + ").");
		}
		this.buffer.put(index * this.dimensions + dimension, value);
	}
	
	/**
	 * Gets the native buffer containing this vertex list which can be used by the GL.
	 * @return
	 */
	public ByteBuffer getBuffer()
	{
		return buffer;
	}
}
