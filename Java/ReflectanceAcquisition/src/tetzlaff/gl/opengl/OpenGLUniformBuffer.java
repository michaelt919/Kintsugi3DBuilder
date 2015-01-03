package tetzlaff.gl.opengl;

import java.nio.ByteBuffer;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;

public class OpenGLUniformBuffer extends OpenGLBuffer 
{

	public OpenGLUniformBuffer(int usage) 
	{
		super(usage);
	}

	public OpenGLUniformBuffer() 
	{
		this(GL_STATIC_DRAW);
	}
	
	public OpenGLUniformBuffer(ByteBuffer data, int usage)
	{
		super(data, usage);
	}
	
	public OpenGLUniformBuffer(ByteVertexList data, int usage)
	{
		super(data.getBuffer(), usage);
	}
	
	public OpenGLUniformBuffer(ShortVertexList data, int usage)
	{
		super(data.getBuffer(), usage);
	}
	
	public OpenGLUniformBuffer(IntVertexList data, int usage)
	{
		super(data.getBuffer(), usage);
	}
	
	public OpenGLUniformBuffer(FloatVertexList data, int usage)
	{
		super(data.getBuffer(), usage);
	}
	
	public OpenGLUniformBuffer(DoubleVertexList data, int usage)
	{
		super(data.getBuffer(), usage);
	}
	
	public OpenGLUniformBuffer(ByteVertexList data)
	{
		this(data, GL_STATIC_DRAW);
	}
	
	public OpenGLUniformBuffer(ShortVertexList data)
	{
		this(data, GL_STATIC_DRAW);
	}
	
	public OpenGLUniformBuffer(IntVertexList data)
	{
		this(data, GL_STATIC_DRAW);
	}
	
	public OpenGLUniformBuffer(FloatVertexList data)
	{
		this(data, GL_STATIC_DRAW);
	}
	
	public OpenGLUniformBuffer(DoubleVertexList data)
	{
		this(data, GL_STATIC_DRAW);
	}

	@Override
	protected int getBufferTarget() 
	{
		return GL_UNIFORM_BUFFER;
	}

}
