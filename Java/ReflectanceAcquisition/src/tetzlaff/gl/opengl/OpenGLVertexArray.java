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
import static org.lwjgl.opengl.GL30.*;

import tetzlaff.gl.Resource;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.exceptions.NoSpecifiedVertexBuffersException;

class OpenGLVertexArray implements Resource
{
	protected final OpenGLContext context;
	
	private boolean usesIndexing = false;
	private int vaoId;
	private int count = Integer.MAX_VALUE;

	OpenGLVertexArray(OpenGLContext context) 
	{
		this.context = context;
		this.vaoId = glGenVertexArrays();
		this.context.openGLErrorCheck();
	}
	
	void bind()
	{
		glBindVertexArray(this.vaoId);
		this.context.openGLErrorCheck();
	}
	
	void addVertexBuffer(int attributeIndex, VertexBuffer<OpenGLContext> buffer)
	{
		if (buffer instanceof OpenGLVertexBuffer)
		{
			if (usesIndexing)
			{
				throw new IllegalStateException("Cannot add a vertex attribute without an index buffer: this VAO already contains other vertex buffers which use index buffers.");
			}
			else
			{
				glBindVertexArray(this.vaoId);
				this.context.openGLErrorCheck();
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
				this.context.openGLErrorCheck();
				((OpenGLVertexBuffer)buffer).useAsVertexAttribute(attributeIndex);
				this.count = Math.min(this.count, buffer.count());
			}
		}
		else
		{
			throw new IllegalArgumentException("'buffer' must be of type OpenGLVertexBuffer.");
		}
	}
	
	void draw(int primitiveMode)
	{
		if (count == Integer.MAX_VALUE)
		{
			throw new NoSpecifiedVertexBuffersException("No vertex buffers were specified for the vertex array.");
		}
		else
		{
			glBindVertexArray(this.vaoId);
			this.context.openGLErrorCheck();
			if (usesIndexing)
			{
				glDrawElements(primitiveMode, this.count, GL_UNSIGNED_INT, 0);
				this.context.openGLErrorCheck();
			}
			else
			{
				glDrawArrays(primitiveMode, 0, this.count);
				this.context.openGLErrorCheck();
			}
		}
	}

	@Override
	public void delete()
	{
		glDeleteVertexArrays(this.vaoId);
		this.context.openGLErrorCheck();
	}
}
