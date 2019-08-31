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

import java.nio.ByteBuffer;

import tetzlaff.gl.core.Contextual;
import tetzlaff.gl.core.Resource;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

abstract class OpenGLBuffer implements Contextual<OpenGLContext>, Resource
{
    protected final OpenGLContext context;

    private final int bufferId;
    private final int usage;

    OpenGLBuffer(OpenGLContext context, int usage)
    {
        this.context = context;
        this.bufferId = glGenBuffers();
        OpenGLContext.errorCheck();
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
        OpenGLContext.errorCheck();
        glBufferData(this.getBufferTarget(), data, this.usage);
        OpenGLContext.errorCheck();
        return this;
    }

    void bind()
    {
        glBindBuffer(this.getBufferTarget(), this.bufferId);
        OpenGLContext.errorCheck();
    }

    void bindToIndex(int index)
    {
        glBindBufferBase(this.getBufferTarget(), index, this.bufferId);
        OpenGLContext.errorCheck();
    }

    @Override
    public void close()
    {
        glDeleteBuffers(this.bufferId);
        OpenGLContext.errorCheck();
    }
}
