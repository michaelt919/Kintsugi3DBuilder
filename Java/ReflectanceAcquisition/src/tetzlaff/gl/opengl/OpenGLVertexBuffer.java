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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

class OpenGLVertexBuffer extends OpenGLBuffer implements VertexBuffer<OpenGLContext>
{
	private int count;
	private int vertexSize;
	private int vertexType;
	private boolean normalize;
	
	OpenGLVertexBuffer(OpenGLContext context, int usage) 
	{
		super(context, usage);
		this.count = 0;
	}
	
	OpenGLVertexBuffer(OpenGLContext context) 
	{
		this(context, GL_STATIC_DRAW);
	}

	@Override
	protected int getBufferTarget() 
	{
		return GL_ARRAY_BUFFER;
	}
	
	@Override
	public int count()
	{
		return this.count;
	}
	
	@Override
	public OpenGLVertexBuffer setData(ByteVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_BYTE : GL_BYTE;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(ShortVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_SHORT : GL_SHORT;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(IntVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_INT : GL_INT;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(FloatVertexList data, boolean normalize)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_FLOAT;
		this.normalize = normalize;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(DoubleVertexList data, boolean normalize)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_DOUBLE;
		this.normalize = normalize;
		return this;
	}
	
	void useAsVertexAttribute(int attribIndex)
	{
		this.bind();
		glEnableVertexAttribArray(attribIndex);
		this.context.openGLErrorCheck();
		if (this.vertexType == GL_FLOAT || this.vertexType == GL_DOUBLE)
		{
			glVertexAttribPointer(attribIndex, this.vertexSize, this.vertexType, this.normalize, 0, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glVertexAttribIPointer(attribIndex, this.vertexSize, this.vertexType, 0, 0);
			this.context.openGLErrorCheck();
		}
	}
}
