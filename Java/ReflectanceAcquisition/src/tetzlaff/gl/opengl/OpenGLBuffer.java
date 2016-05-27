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
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import tetzlaff.gl.Contextual;
import tetzlaff.gl.Resource;

abstract class OpenGLBuffer implements Contextual<OpenGLContext>, Resource
{
	protected final OpenGLContext context;
	
	private int bufferId;
	private int usage;

	OpenGLBuffer(OpenGLContext context, int usage) 
	{
		this.context = context;
		this.bufferId = glGenBuffers();
		this.context.openGLErrorCheck();
		this.usage = usage;
	}
	
	@Override
	public OpenGLContext getContext()
	{
		return this.context;
	}
	
	int getBufferId()
	{
		return this.bufferId;
	}
	
	abstract int getBufferTarget();
	
	OpenGLBuffer setData(ByteBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(ShortBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(IntBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(FloatBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(DoubleBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	void bind()
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
	}
	
	void bindToIndex(int index)
	{
		glBindBufferBase(this.getBufferTarget(), index, this.bufferId);
		this.context.openGLErrorCheck();
	}

	@Override
	public void delete()
	{
		glDeleteBuffers(this.bufferId);
		this.context.openGLErrorCheck();
	}
}
