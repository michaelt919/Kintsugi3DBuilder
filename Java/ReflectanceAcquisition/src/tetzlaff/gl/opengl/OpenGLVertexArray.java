package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.VertexArray;
import tetzlaff.gl.exceptions.NoSpecifiedVertexBuffersException;

public class OpenGLVertexArray implements OpenGLResource, VertexArray<OpenGLVertexBuffer, OpenGLIndexBuffer>
{
	private boolean usesIndexing = false;
	private int vaoId;
	private int count = Integer.MAX_VALUE;
	private List<OpenGLResource> ownedResources;

	public OpenGLVertexArray() 
	{
		this.vaoId = glGenVertexArrays();
		openGLErrorCheck();
		this.ownedResources = new ArrayList<OpenGLResource>();
	}
	
	void bind()
	{
		glBindVertexArray(this.vaoId);
		openGLErrorCheck();
	}
	
	@Override
	public void addVertexBuffer(int attributeIndex, OpenGLVertexBuffer buffer, boolean owned)
	{
		if (usesIndexing)
		{
			throw new IllegalStateException("Cannot add a vertex attribute without an index buffer: this VAO already contains other vertex buffers which use index buffers.");
		}
		else
		{
			if (owned)
			{
				this.ownedResources.add(buffer);
			}
			
			glBindVertexArray(this.vaoId);
			openGLErrorCheck();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			openGLErrorCheck();
			buffer.useAsVertexAttribute(attributeIndex);
			this.count = Math.min(this.count, buffer.count());
		}
	}
	
	@Override
	public void addVertexBuffer(int attributeIndex, OpenGLVertexBuffer buffer)
	{
		this.addVertexBuffer(attributeIndex, buffer, false);
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
		
		for (OpenGLResource resource : ownedResources)
		{
			resource.delete();
		}
	}
}
