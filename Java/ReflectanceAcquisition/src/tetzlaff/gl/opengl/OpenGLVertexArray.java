package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.VertexArray;
import tetzlaff.gl.exceptions.NoSpecifiedVertexBuffersException;

public class OpenGLVertexArray implements OpenGLResource, VertexArray<OpenGLVertexBuffer, OpenGLIndexBuffer>
{
	private boolean usesIndexing = false;
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
	public void addVertexBuffer(int attributeIndex, OpenGLVertexBuffer buffer, OpenGLIndexBuffer indexBuffer)
	{
		if (!usesIndexing && count < Integer.MAX_VALUE)
		{
			throw new IllegalStateException("Cannot add a vertex attribute with an index buffer: this VAO already contains other vertex buffers which are not associated with an index buffer.");
		}
		else
		{
			this.usesIndexing = true;
			glBindVertexArray(this.vaoId);
			openGLErrorCheck();
			indexBuffer.bind();
			buffer.useAsVertexAttribute(attributeIndex);
			this.count = Math.min(this.count, indexBuffer.count());
		}
	}
	
	@Override
	public void addVertexBuffer(int attributeIndex, OpenGLVertexBuffer buffer)
	{
		if (usesIndexing)
		{
			throw new IllegalStateException("Cannot add a vertex attribute without an index buffer: this VAO already contains other vertex buffers which use index buffers.");
		}
		else
		{
			glBindVertexArray(this.vaoId);
			openGLErrorCheck();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			openGLErrorCheck();
			buffer.useAsVertexAttribute(attributeIndex);
			this.count = Math.min(this.count, buffer.count());
		}
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
			if (usesIndexing)
			{
				glDrawElements(primitiveMode, this.count, GL_UNSIGNED_INT, 0);
			}
			else
			{
				glDrawArrays(primitiveMode, 0, this.count);
			}
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
