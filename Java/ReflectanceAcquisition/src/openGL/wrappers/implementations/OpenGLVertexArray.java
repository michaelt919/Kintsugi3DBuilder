package openGL.wrappers.implementations;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static openGL.OpenGLHelper.*;
import openGL.wrappers.exceptions.NoSpecifiedVertexBuffersException;
import openGL.wrappers.interfaces.VertexArray;
import openGL.wrappers.interfaces.VertexBuffer;

public class OpenGLVertexArray implements VertexArray
{
	private int vaoId;
	private int count = Integer.MAX_VALUE;

	public OpenGLVertexArray() 
	{
		this.vaoId = glGenVertexArrays();
		openGLErrorCheck();
	}
	
	@Override
	public void bind()
	{
		glBindVertexArray(this.vaoId);
		openGLErrorCheck();
	}
	
	@Override
	public void addVertexBuffer(int attributeIndex, VertexBuffer buffer)
	{
		glBindVertexArray(this.vaoId);
		openGLErrorCheck();
		buffer.useAsVertexAttribute(attributeIndex);
		this.count = Math.min(this.count, buffer.count());
	}
	
	@Override
	public void draw(int primitiveMode)
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
