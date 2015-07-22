package tetzlaff.gl;

public interface VertexArray<ContextType extends Context> extends Resource
{
	void addVertexBuffer(int attributeIndex, VertexBuffer<ContextType> buffer, boolean owned);
	void addVertexBuffer(int attributeIndex, VertexBuffer<ContextType> vertexBuffer);
}
