package tetzlaff.gl;

import tetzlaff.gl.opengl.OpenGLResource;

public interface VertexArray<VertexBufferType extends VertexBuffer, IndexBufferType extends IndexBuffer>
{
	void addVertexBuffer(int attributeIndex, VertexBufferType vertexBuffer, IndexBufferType indexBuffer);
	void addVertexBuffer(int attributeIndex, VertexBufferType vertexBuffer);
}
