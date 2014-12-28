package tetzlaff.gl;

public interface VertexArray<VertexBufferType extends VertexBuffer, IndexBufferType extends IndexBuffer>
{
	void addVertexBuffer(int attributeIndex, VertexBufferType buffer, boolean owned);
	void addVertexBuffer(int attributeIndex, VertexBufferType vertexBuffer);
}
