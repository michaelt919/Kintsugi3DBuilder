package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL15.*;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.IndexBuffer;

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
	
	private static IntBuffer convertToIntBuffer(int[] data)
	{
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
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
		super.setData(convertToIntBuffer(data));
		this.count = data.length;
		return this;
	}
}
