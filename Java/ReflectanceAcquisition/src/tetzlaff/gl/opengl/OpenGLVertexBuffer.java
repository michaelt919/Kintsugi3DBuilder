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

public class OpenGLVertexBuffer extends OpenGLBuffer implements VertexBuffer
{
	private int count;
	private int vertexSize;
	private int vertexType;
	private boolean normalize;
	
	public OpenGLVertexBuffer(int usage) 
	{
		super(usage);
		this.count = 0;
	}
	
	public OpenGLVertexBuffer() 
	{
		this(GL_STATIC_DRAW);
	}
	
	public OpenGLVertexBuffer(ByteVertexList data, boolean unsigned, int usage)
	{
		super(data.getBuffer(), usage);
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_BYTE : GL_BYTE;
	}
	
	public OpenGLVertexBuffer(ShortVertexList data, boolean unsigned, int usage)
	{
		super(data.getBuffer(), usage);
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_SHORT : GL_SHORT;
	}
	
	public OpenGLVertexBuffer(IntVertexList data, boolean unsigned, int usage)
	{
		super(data.getBuffer(), usage);
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_INT : GL_INT;
	}
	
	public OpenGLVertexBuffer(FloatVertexList data, boolean normalize, int usage)
	{
		super(data.getBuffer(), usage);
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_FLOAT;
		this.normalize = normalize;
	}
	
	public OpenGLVertexBuffer(DoubleVertexList data, boolean normalize, int usage)
	{
		super(data.getBuffer(), usage);
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_DOUBLE;
		this.normalize = normalize;
	}
	
	public OpenGLVertexBuffer(ByteVertexList data, boolean unsigned)
	{
		this(data, unsigned, GL_STATIC_DRAW);
	}
	
	public OpenGLVertexBuffer(ShortVertexList data, boolean unsigned)
	{
		this(data, unsigned, GL_STATIC_DRAW);
	}
	
	public OpenGLVertexBuffer(IntVertexList data, boolean unsigned)
	{
		this(data, unsigned, GL_STATIC_DRAW);
	}
	
	public OpenGLVertexBuffer(FloatVertexList data, boolean normalize)
	{
		this(data, normalize, GL_STATIC_DRAW);
	}
	
	public OpenGLVertexBuffer(DoubleVertexList data, boolean normalize)
	{
		this(data, normalize, GL_STATIC_DRAW);
	}
	
	public OpenGLVertexBuffer(ByteVertexList data)
	{
		this(data, false);
	}
	
	public OpenGLVertexBuffer(ShortVertexList data)
	{
		this(data, false);
	}
	
	public OpenGLVertexBuffer(IntVertexList data)
	{
		this(data, false);
	}
	
	public OpenGLVertexBuffer(FloatVertexList data)
	{
		this(data, false);
	}
	
	public OpenGLVertexBuffer(DoubleVertexList data)
	{
		this(data, false);
	}
	
	public static OpenGLVertexBuffer createRectangle()
	{
		return new OpenGLVertexBuffer(new FloatVertexList(2, 4, new float[] { -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f }));
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
	public void setData(ByteVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_BYTE : GL_BYTE;
	}

	@Override
	public void setData(ShortVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_SHORT : GL_SHORT;
	}

	@Override
	public void setData(IntVertexList data, boolean unsigned)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = unsigned ? GL_UNSIGNED_INT : GL_INT;
	}

	@Override
	public void setData(FloatVertexList data, boolean normalize)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_FLOAT;
		this.normalize = normalize;
	}

	@Override
	public void setData(DoubleVertexList data, boolean normalize)
	{
		super.setData(data.getBuffer());
		this.count = data.count;
		this.vertexSize = data.dimensions;
		this.vertexType = GL_DOUBLE;
		this.normalize = normalize;
	}
	
	void useAsVertexAttribute(int attribIndex)
	{
		this.bind();
		glEnableVertexAttribArray(attribIndex);
		openGLErrorCheck();
		if (this.vertexType == GL_FLOAT || this.vertexType == GL_DOUBLE)
		{
			glVertexAttribPointer(attribIndex, this.vertexSize, this.vertexType, this.normalize, 0, 0);
			openGLErrorCheck();
		}
		else
		{
			glVertexAttribIPointer(attribIndex, this.vertexSize, this.vertexType, 0, 0);
			openGLErrorCheck();
		}
	}
}
