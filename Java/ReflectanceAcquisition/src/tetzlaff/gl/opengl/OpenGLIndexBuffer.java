package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.IndexBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public class OpenGLIndexBuffer extends OpenGLBuffer implements IndexBuffer
{
	private int count;
	
	public OpenGLIndexBuffer(int usage) 
	{
		super(usage);
		this.count = 0;
	}
	
	public OpenGLIndexBuffer() 
	{
		this(GL_STATIC_DRAW);
	}
	
	private static IntBuffer convertToIntBuffer(int[] data)
	{
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
	
	public OpenGLIndexBuffer(int[] data, int usage)
	{
		super(convertToIntBuffer(data), usage);
		this.count = data.length;
	}
	
	public OpenGLIndexBuffer(int[] data)
	{
		this(data, GL_STATIC_DRAW);
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
	public void setData(int[] data)
	{
		super.setData(convertToIntBuffer(data));
		this.count = data.length;
	}
}
