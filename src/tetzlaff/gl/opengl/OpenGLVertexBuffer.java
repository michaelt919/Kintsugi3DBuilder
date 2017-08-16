package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

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
        this.vertexType = context.getDataTypeConstant(data.getDataType());
        this.normalize = normalize;
        return this;
    }

    void useAsVertexAttribute(int attribIndex)
    {
        this.bind();
        glEnableVertexAttribArray(attribIndex);
        this.context.openGLErrorCheck();
        if (this.vertexType == GL_FLOAT || this.vertexType == GL_DOUBLE)
        {
            glVertexAttribPointer(attribIndex, this.vertexSize, this.vertexType, this.normalize, 0, 0);
            this.context.openGLErrorCheck();
        }
        else
        {
            glVertexAttribIPointer(attribIndex, this.vertexSize, this.vertexType, 0, 0);
            this.context.openGLErrorCheck();
        }
    }
}
