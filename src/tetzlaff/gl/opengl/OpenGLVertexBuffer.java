/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.opengl;

import tetzlaff.gl.core.VertexBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

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
    public OpenGLVertexBuffer setData(NativeVectorBuffer data, boolean normalize)
    {
        super.setData(data.getBuffer());
        this.count = data.getCount();
        this.vertexSize = data.getDimensions();
        this.vertexType = OpenGLContext.getDataTypeConstant(data.getDataType());
        this.normalize = normalize;
        return this;
    }

    void useAsVertexAttribute(int attribIndex)
    {
        this.bind();
        glEnableVertexAttribArray(attribIndex);
        OpenGLContext.errorCheck();
        if (this.vertexType == GL_FLOAT || this.vertexType == GL_DOUBLE)
        {
            glVertexAttribPointer(attribIndex, this.vertexSize, this.vertexType, this.normalize, 0, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            glVertexAttribIPointer(attribIndex, this.vertexSize, this.vertexType, 0, 0);
            OpenGLContext.errorCheck();
        }
    }
}
