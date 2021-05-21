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

package tetzlaff.gl.opengl;

import tetzlaff.gl.core.Resource;
import tetzlaff.gl.core.VertexBuffer;
import tetzlaff.gl.exceptions.NoSpecifiedVertexBuffersException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

class OpenGLVertexArray implements Resource
{
    protected final OpenGLContext context;

    private final boolean usesIndexing = false;
    private final int vaoId;
    private int count = Integer.MAX_VALUE;

    OpenGLVertexArray(OpenGLContext context)
    {
        this.context = context;
        this.vaoId = glGenVertexArrays();
        OpenGLContext.errorCheck();
    }

    void bind()
    {
        glBindVertexArray(this.vaoId);
        OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
                OpenGLContext.errorCheck();
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
            OpenGLContext.errorCheck();
            if (usesIndexing)
            {
                glDrawElements(primitiveMode, this.count, GL_UNSIGNED_INT, 0);
                OpenGLContext.errorCheck();
            }
            else
            {
                glDrawArrays(primitiveMode, 0, this.count);
                OpenGLContext.errorCheck();
            }
        }
    }

    @Override
    public void close()
    {
        glDeleteVertexArrays(this.vaoId);
        OpenGLContext.errorCheck();
    }
}
