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

import kintsugi3d.gl.core.UniformBuffer;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;

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
    void bindToIndex(int index)
    {
        context.bindUniformBufferToIndex(index, this);
    }

    @Override
    public OpenGLUniformBuffer setData(ByteBuffer data)
    {
        super.setData(data);
        return this;
    }

    @Override
    public OpenGLUniformBuffer setData(ReadonlyNativeVectorBuffer data)
    {
        super.setData(data.getBuffer());
        return this;
    }
}
