package openGL.wrappers.interfaces;

public interface VertexArray extends GLResource
{
	void bind();
	void addVertexBuffer(int attributeIndex, VertexBuffer buffer);
	void draw(int primitiveMode);
}
