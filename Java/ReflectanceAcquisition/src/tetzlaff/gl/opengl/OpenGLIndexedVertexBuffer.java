package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public class OpenGLIndexedVertexBuffer
{
	private OpenGLVertexBuffer vertexBuffer;
	private OpenGLIndexBuffer indexBuffer;
	
	public OpenGLIndexedVertexBuffer(int usage) 
	{
		this.vertexBuffer = new OpenGLVertexBuffer(usage);
		this.indexBuffer = new OpenGLIndexBuffer();
	}
	
	public OpenGLIndexedVertexBuffer() 
	{
		this(GL_STATIC_DRAW);
	}
	
	public OpenGLIndexedVertexBuffer(ByteVertexList data, int[] indices, boolean unsigned, int usage)
	{
		this.vertexBuffer = new OpenGLVertexBuffer(data, unsigned, usage);
		this.indexBuffer = new OpenGLIndexBuffer(indices);
	}
	
	public OpenGLIndexedVertexBuffer(ShortVertexList data, int[] indices, boolean unsigned, int usage)
	{
		this.vertexBuffer = new OpenGLVertexBuffer(data, unsigned, usage);
		this.indexBuffer = new OpenGLIndexBuffer(indices);
	}
	
	public OpenGLIndexedVertexBuffer(IntVertexList data, int[] indices, boolean unsigned, int usage)
	{
		this.vertexBuffer = new OpenGLVertexBuffer(data, unsigned, usage);
		this.indexBuffer = new OpenGLIndexBuffer(indices);
	}
	
	public OpenGLIndexedVertexBuffer(FloatVertexList data, int[] indices, boolean normalize, int usage)
	{
		this.vertexBuffer = new OpenGLVertexBuffer(data, normalize, usage);
		this.indexBuffer = new OpenGLIndexBuffer(indices);
	}
	
	public OpenGLIndexedVertexBuffer(DoubleVertexList data, int[] indices, boolean normalize, int usage)
	{
		this.vertexBuffer = new OpenGLVertexBuffer(data, normalize, usage);
		this.indexBuffer = new OpenGLIndexBuffer(indices);
	}
	
	public OpenGLIndexedVertexBuffer(ByteVertexList data, int[] indices, boolean unsigned)
	{
		this(data, indices, unsigned, GL_STATIC_DRAW);
	}
	
	public OpenGLIndexedVertexBuffer(ShortVertexList data, int[] indices, boolean unsigned)
	{
		this(data, indices, unsigned, GL_STATIC_DRAW);
	}
	
	public OpenGLIndexedVertexBuffer(IntVertexList data, int[] indices, boolean unsigned)
	{
		this(data, indices, unsigned, GL_STATIC_DRAW);
	}
	
	public OpenGLIndexedVertexBuffer(FloatVertexList data, int[] indices, boolean normalize)
	{
		this(data, indices, normalize, GL_STATIC_DRAW);
	}
	
	public OpenGLIndexedVertexBuffer(DoubleVertexList data, int[] indices, boolean normalize)
	{
		this(data, indices, normalize, GL_STATIC_DRAW);
	}
	
	public OpenGLIndexedVertexBuffer(ByteVertexList data, int[] indices)
	{
		this(data, indices, false);
	}
	
	public OpenGLIndexedVertexBuffer(ShortVertexList data, int[] indices)
	{
		this(data, indices, false);
	}
	
	public OpenGLIndexedVertexBuffer(IntVertexList data, int[] indices)
	{
		this(data, indices, false);
	}
	
	public OpenGLIndexedVertexBuffer(FloatVertexList data, int[] indices)
	{
		this(data, indices, false);
	}
	
	public OpenGLIndexedVertexBuffer(DoubleVertexList data, int[] indices)
	{
		this(data, indices, false);
	}
	
	public int count()
	{
		return this.indexBuffer.count();
	}
	
	public void setData(ByteVertexList data, boolean unsigned)
	{
		this.vertexBuffer.setData(data, unsigned);
	}

	public void setData(ShortVertexList data, boolean unsigned)
	{
		this.vertexBuffer.setData(data, unsigned);
	}

	public void setData(IntVertexList data, boolean unsigned)
	{
		this.vertexBuffer.setData(data, unsigned);
	}

	public void setData(FloatVertexList data, boolean normalize)
	{
		this.vertexBuffer.setData(data, normalize);
	}

	public void setData(DoubleVertexList data, boolean normalize)
	{
		this.vertexBuffer.setData(data, normalize);
	}
	
	void useAsVertexAttribute(int attribIndex)
	{
		this.indexBuffer.bind();
		this.vertexBuffer.useAsVertexAttribute(attribIndex);
	}
}
