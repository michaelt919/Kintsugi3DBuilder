/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.opengl;

import kintsugi3d.gl.core.IndexBuffer;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

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

    private static ByteBuffer convertToByteBuffer(int... data)
    {
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length * 4);
        buffer.asIntBuffer().put(data);
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
    public OpenGLIndexBuffer setData(int... data)
    {
        super.setData(convertToByteBuffer(data));
        this.count = data.length;
        return this;
    }
}
