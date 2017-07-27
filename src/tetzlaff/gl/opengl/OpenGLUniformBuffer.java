package tetzlaff.gl.opengl;

import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

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
	public OpenGLUniformBuffer setData(NativeVectorBuffer data)
	{
		super.setData(data.getBuffer());
		return this;
	}
}
