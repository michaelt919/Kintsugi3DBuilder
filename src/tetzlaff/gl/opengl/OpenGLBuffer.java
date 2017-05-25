package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import tetzlaff.gl.Contextual;
import tetzlaff.gl.Resource;

abstract class OpenGLBuffer implements Contextual<OpenGLContext>, Resource
{
	protected final OpenGLContext context;
	
	private int bufferId;
	private int usage;

	OpenGLBuffer(OpenGLContext context, int usage) 
	{
		this.context = context;
		this.bufferId = glGenBuffers();
		this.context.openGLErrorCheck();
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
	
	OpenGLBuffer setData(ByteBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(ShortBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(IntBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(FloatBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	OpenGLBuffer setData(DoubleBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		this.context.openGLErrorCheck();
		return this;
	}
	
	void bind()
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		this.context.openGLErrorCheck();
	}
	
	void bindToIndex(int index)
	{
		glBindBufferBase(this.getBufferTarget(), index, this.bufferId);
		this.context.openGLErrorCheck();
	}

	@Override
	public void close()
	{
		glDeleteBuffers(this.bufferId);
		this.context.openGLErrorCheck();
	}
}
