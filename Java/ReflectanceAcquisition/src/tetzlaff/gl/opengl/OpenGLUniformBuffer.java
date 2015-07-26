package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;


import java.nio.ByteBuffer;

import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

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
	public OpenGLUniformBuffer setData(ByteBuffer data)
	{
		super.setData(data);
		return this;
	}
	
	@Override
	public OpenGLUniformBuffer setData(ByteVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(ShortVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(IntVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(FloatVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(DoubleVertexList data)
	{
		super.setData(data.getBuffer());
		return this;
	}
}
