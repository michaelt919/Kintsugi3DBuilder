package openGL.wrappers.implementations;

import static openGL.OpenGLHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import openGL.wrappers.interfaces.GLResource;

public abstract class OpenGLBuffer implements GLResource
{
	public /*TODO*/ int bufferId;
	private int usage;

	protected OpenGLBuffer(int usage) 
	{
		this.bufferId = glGenBuffers();
		openGLErrorCheck();
		this.usage = usage;
	}

	protected OpenGLBuffer(ByteBuffer data, int usage) 
	{
		this(usage);
		this.setData(data);
	}

	protected OpenGLBuffer(ShortBuffer data, int usage) 
	{
		this(usage);
		this.setData(data);
	}

	protected OpenGLBuffer(IntBuffer data, int usage) 
	{
		this(usage);
		this.setData(data);
	}

	protected OpenGLBuffer(FloatBuffer data, int usage) 
	{
		this(usage);
		this.setData(data);
	}

	protected OpenGLBuffer(DoubleBuffer data, int usage) 
	{
		this(usage);
		this.setData(data);
	}
	
	protected abstract int getBufferTarget();
	
	protected void setData(ByteBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		openGLErrorCheck();
	}
	
	protected void setData(ShortBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		openGLErrorCheck();
	}
	
	protected void setData(IntBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		openGLErrorCheck();
	}
	
	protected void setData(FloatBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		openGLErrorCheck();
	}
	
	protected void setData(DoubleBuffer data)
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		openGLErrorCheck();
		glBufferData(this.getBufferTarget(), data, this.usage);
		openGLErrorCheck();
	}
	
	protected void bind()
	{
		glBindBuffer(this.getBufferTarget(), this.bufferId);
		openGLErrorCheck();
	}

	@Override
	public void delete()
	{
		glDeleteBuffers(this.bufferId);
		openGLErrorCheck();
	}
}
