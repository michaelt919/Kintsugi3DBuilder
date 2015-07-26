package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

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
	public OpenGLVertexBuffer setData(ByteVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_BYTE : GL_BYTE;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(ShortVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_SHORT : GL_SHORT;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(IntVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_INT : GL_INT;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(FloatVertexList data, boolean normalize)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_FLOAT;
		this.normalize = normalize;
		return this;
	}

	@Override
	public OpenGLVertexBuffer setData(DoubleVertexList data, boolean normalize)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_DOUBLE;
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
