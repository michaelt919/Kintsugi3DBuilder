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
package tetzlaff.gl;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

/**
 * An interface for a vertex buffer object that can provide data to be used for rendering.
 * A vertex buffer should a series of vertex attributes that can be organized into "primitives" for rendering.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface VertexBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	/**
	 * Gets the number of vertices in the vertex buffer.
	 * @return The number of vertices in the vertex buffer.
	 */
	int count();
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 8-bit integers.
	 * @param unsigned Whether or not the integers should be treated as unsigned integers.
	 * @return The calling object.
	 */
	VertexBuffer<ContextType> setData(ByteVertexList data, boolean unsigned);
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 16-bit integers.
	 * @param unsigned Whether or not the integers should be treated as unsigned integers.
	 * @return The calling object.
	 */
	VertexBuffer<ContextType> setData(ShortVertexList data, boolean unsigned);
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 32-bit integers.
	 * @param unsigned Whether or not the integers should be treated as unsigned integers.
	 * @return The calling object.
	 */
	VertexBuffer<ContextType> setData(IntVertexList data, boolean unsigned);
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 32-bit floating-point numbers.
	 * @param unsigned Whether or not each vertex should be automatically normalized.
	 * @return The calling object.
	 */
	VertexBuffer<ContextType> setData(FloatVertexList data, boolean normalize);
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 64-bit floating-point numbers.
	 * @param unsigned Whether or not each vertex should be automatically normalized.
	 * @return The calling object.
	 */
	VertexBuffer<ContextType> setData(DoubleVertexList data, boolean normalize);
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 8-bit signed integers.
	 * @return The calling object.
	 */
	default VertexBuffer<ContextType> setData(ByteVertexList data)
	{
		return this.setData(data, false);
	}
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 16-bit signed integers.
	 * @return The calling object.
	 */
	default VertexBuffer<ContextType> setData(ShortVertexList data)
	{
		return this.setData(data, false);
	}
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 32-bit signed integers.
	 * @return The calling object.
	 */
	default VertexBuffer<ContextType> setData(IntVertexList data)
	{
		return this.setData(data, false);
	}
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 32-bit, unnormalized floating-point numbers.
	 * @return The calling object.
	 */
	default VertexBuffer<ContextType> setData(FloatVertexList data)
	{
		return this.setData(data, false);
	}
	
	/**
	 * Sets the content of the vertex buffer.
	 * @param data A list of the values to be stored as 64-bit, unnormalized floating-point numbers.
	 * @return The calling object.
	 */
	default VertexBuffer<ContextType> setData(DoubleVertexList data)
	{
		return this.setData(data, false);
	}
}
