package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.VertexArray;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.exceptions.NoSpecifiedVertexBuffersException;

public class OpenGLVertexArray implements VertexArray<OpenGLVertexBuffer>
{
	private int vaoId;
	private int count = Integer.MAX_VALUE;

	public OpenGLVertexArray() 
	{
		this.vaoId = glGenVertexArrays();
		openGLErrorCheck();
	}
	
	void bind()
	{
		glBindVertexArray(this.vaoId);
		openGLErrorCheck();
	}
	
	@Override
	public void addVertexBuffer(int attributeIndex, OpenGLVertexBuffer buffer)
	{
		glBindVertexArray(this.vaoId);
		openGLErrorCheck();
		buffer.useAsVertexAttribute(attributeIndex);
		this.count = Math.min(this.count, buffer.count());
	}
	
	void draw(int primitiveMode)
	{
		if (count == Integer.MAX_VALUE)
		{
			throw new NoSpecifiedVertexBuffersException("No vertex buffers were specified for the vertex array.");
		}
		else
		{
			glBindVertexArray(this.vaoId);
			openGLErrorCheck();
			glDrawArrays(primitiveMode, 0, this.count);
			openGLErrorCheck();
		}
	}

	@Override
	public void delete()
	{
		glDeleteVertexArrays(this.vaoId);
		openGLErrorCheck();
	}
}
