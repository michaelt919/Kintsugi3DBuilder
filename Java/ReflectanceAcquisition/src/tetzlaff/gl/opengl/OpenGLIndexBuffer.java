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
package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL15.*;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.IndexBuffer;

class OpenGLIndexBuffer extends OpenGLBuffer implements IndexBuffer<OpenGLContext>
{
	private int count;
	
	OpenGLIndexBuffer(OpenGLContext context, int usage) 
	{
		super(context, usage);
		this.count = 0;
	}
	
	OpenGLIndexBuffer(OpenGLContext context) 
	{
		this(context, GL_STATIC_DRAW);
	}
	
	private static IntBuffer convertToIntBuffer(int[] data)
	{
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	@Override
	protected int getBufferTarget() 
	{
		return GL_ELEMENT_ARRAY_BUFFER;
	}
	
	@Override
	public int count()
	{
		return this.count;
	}

	@Override
	public OpenGLIndexBuffer setData(int[] data)
	{
		super.setData(convertToIntBuffer(data));
		this.count = data.length;
		return this;
	}
}
