package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.ByteBuffer;

import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.nativelist.NativeByteVectorList;
import tetzlaff.gl.nativelist.NativeDoubleVectorList;
import tetzlaff.gl.nativelist.NativeFloatVectorList;
import tetzlaff.gl.nativelist.NativeIntVectorList;
import tetzlaff.gl.nativelist.NativeShortVectorList;

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
	public OpenGLUniformBuffer setData(NativeByteVectorList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(NativeShortVectorList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(NativeIntVectorList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(NativeFloatVectorList data)
	{
		super.setData(data.getBuffer());
		return this;
	}

	@Override
	public OpenGLUniformBuffer setData(NativeDoubleVectorList data)
	{
		super.setData(data.getBuffer());
		return this;
	}
}
