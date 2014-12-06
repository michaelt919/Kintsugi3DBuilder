package tetzlaff.gl;

import tetzlaff.gl.opengl.OpenGLResource;

public interface VertexArray<VertexBufferType extends VertexBuffer> extends OpenGLResource
{
	void addVertexBuffer(int attributeIndex, VertexBufferType buffer);
}
