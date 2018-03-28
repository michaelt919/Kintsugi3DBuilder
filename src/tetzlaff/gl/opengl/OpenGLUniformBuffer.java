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
