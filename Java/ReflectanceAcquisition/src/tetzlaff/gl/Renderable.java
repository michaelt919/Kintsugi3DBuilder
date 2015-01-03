package tetzlaff.gl;

import tetzlaff.gl.helpers.*;

public interface Renderable<
	ProgramType extends Program<?, ? super TextureType, ?>, 
	VertexBufferType extends VertexBuffer,
	FramebufferType extends Framebuffer, TextureType >
{
	ProgramType program();

	void draw(PrimitiveMode primitiveMode, FramebufferType framebuffer);

	void draw(PrimitiveMode primitiveMode, FramebufferType framebuffer, int x, int y,
			int width, int height);

	void draw(PrimitiveMode primitiveMode, FramebufferType framebuffer, int width,
			int height);
	
	void draw(PrimitiveMode primitiveMode, Context context);
	
	void draw(PrimitiveMode primitiveMode, Context context, int x, int y, int width, int height);

	void draw(PrimitiveMode primitiveMode, Context context, int width, int height);
	
	void setVertexAttrib(String name, DoubleVector4 value);

	void setVertexAttrib(String name, DoubleVector3 value);

	void setVertexAttrib(String name, DoubleVector2 value);

	void setVertexAttrib(String name, double value);

	void setVertexAttrib(String name, Vector4 value);

	void setVertexAttrib(String name, Vector3 value);

	void setVertexAttrib(String name, Vector2 value);

	void setVertexAttrib(String name, float value);

	void setVertexAttrib(String name, IntVector4 value);

	void setVertexAttrib(String name, IntVector3 value);

	void setVertexAttrib(String name, IntVector2 value);

	void setVertexAttrib(String name, int value);

	void setVertexAttrib(int location, DoubleVector4 value);

	void setVertexAttrib(int location, DoubleVector3 value);

	void setVertexAttrib(int location, DoubleVector2 value);

	void setVertexAttrib(int location, double value);

	void setVertexAttrib(int location, Vector4 value);

	void setVertexAttrib(int location, Vector3 value);

	void setVertexAttrib(int location, Vector2 value);

	void setVertexAttrib(int location, float value);

	void setVertexAttrib(int location, IntVector4 value);

	void setVertexAttrib(int location, IntVector3 value);

	void setVertexAttrib(int location, IntVector2 value);

	void setVertexAttrib(int location, int value);

	void addVertexBuffer(int location, VertexBufferType buffer, boolean owned);

	void addVertexBuffer(String name, VertexBufferType buffer, boolean owned);

	void addVertexBuffer(int location, VertexBufferType buffer);

	void addVertexBuffer(String name, VertexBufferType buffer);
	
	void addVertexMesh(String vertexName, String texCoordName, String normalName, VertexMesh mesh);
}
