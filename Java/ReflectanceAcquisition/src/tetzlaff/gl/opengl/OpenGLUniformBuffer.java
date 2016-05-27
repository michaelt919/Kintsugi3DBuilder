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
import static org.lwjgl.opengl.GL31.*;


import java.nio.ByteBuffer;

import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

class OpenGLUniformBuffer extends OpenGLBuffer implements UniformBuffer<OpenGLContext>
{
	OpenGLUniformBuffer(OpenGLContext context, int usage) 
	{
		super(context, usage);
	}

	OpenGLUniformBuffer(OpenGLContext context) 
	{
		this(context, GL_STATIC_DRAW);
	}

	@Override
	protected int getBufferTarget() 
	{
		return GL_UNIFORM_BUFFER;
	}
	
	@Override
	public OpenGLUniformBuffer setData(ByteBuffer data)
	{
		super.setData(data);
		return this;
	}
	
	@Override
	public OpenGLUniformBuffer setData(ByteVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(ShortVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(IntVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(FloatVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(DoubleVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}
}
