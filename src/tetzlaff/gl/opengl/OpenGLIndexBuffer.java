package tetzlaff.gl.opengl;

import java.nio.ByteBuffer;

import org.lwjgl.*;
import tetzlaff.gl.IndexBuffer;

import static org.lwjgl.opengl.GL15.*;

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

    private static ByteBuffer convertToByteBuffer(int[] data)
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
    public OpenGLIndexBuffer setData(int[] data)
    {
        super.setData(convertToByteBuffer(data));
        this.count = data.length;
        return this;
    }
}
