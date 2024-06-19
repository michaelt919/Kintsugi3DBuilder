/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.opengl;

import java.nio.ByteBuffer;

import kintsugi3d.gl.core.ContextBound;
import kintsugi3d.gl.core.Resource;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

abstract class OpenGLBuffer implements ContextBound<OpenGLContext>, Resource
{
    protected final OpenGLContext context;

    private final int bufferId;
    private final int usage;
    private boolean hasData = false;
    private boolean closed = false;

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

    boolean hasData()
    {
        return hasData;
    }

    OpenGLBuffer setData(ByteBuffer data)
    {
        if (data.remaining() > 0) // make sure there is actually data to set (lwjgl's glBufferData uses data.remaining() as the "size" parameter)
        {
            glBindBuffer(this.getBufferTarget(), this.bufferId);
            OpenGLContext.errorCheck();
            glBufferData(this.getBufferTarget(), data, this.usage);
            OpenGLContext.errorCheck();

            // fix for MacOS which segfaults if using a buffer with no data
            hasData = true;
        }

        return this;
    }

    void bind()
    {
        if (hasData) // fix for MacOS which segfaults if using a buffer with no data
        {
            glBindBuffer(this.getBufferTarget(), this.bufferId);
            OpenGLContext.errorCheck();
        }
        else
        {
            glBindBuffer(this.getBufferTarget(), 0);
            OpenGLContext.errorCheck();
        }
    }

    void bindToIndex(int index)
    {
        if (hasData) // fix for MacOS which segfaults if using a buffer with no data
        {
            glBindBufferBase(this.getBufferTarget(), index, this.bufferId);
            OpenGLContext.errorCheck();
        }
        else
        {
            glBindBufferBase(this.getBufferTarget(), index, 0);
            OpenGLContext.errorCheck();
        }
    }

    @Override
    public void close()
    {
        if (!closed)
        {
            glDeleteBuffers(this.bufferId);
            OpenGLContext.errorCheck();
            closed = true;
            hasData = false;
        }
    }
}
