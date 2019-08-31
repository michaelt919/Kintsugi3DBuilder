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

import tetzlaff.gl.core.UniformBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

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
    public OpenGLUniformBuffer setData(NativeVectorBuffer data)
    {
        super.setData(data.getBuffer());
        return this;
    }
}
