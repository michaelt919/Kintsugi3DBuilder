package tetzlaff.gl;

import tetzlaff.gl.helpers.*;
import tetzlaff.gl.opengl.OpenGLResource;

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
	
	boolean setVertexAttrib(String name, DoubleVector4 value);

	boolean setVertexAttrib(String name, DoubleVector3 value);

	boolean setVertexAttrib(String name, DoubleVector2 value);

	boolean setVertexAttrib(String name, double value);

	boolean setVertexAttrib(String name, Vector4 value);

	boolean setVertexAttrib(String name, Vector3 value);

	boolean setVertexAttrib(String name, Vector2 value);

	boolean setVertexAttrib(String name, float value);

	boolean setVertexAttrib(String name, IntVector4 value);

	boolean setVertexAttrib(String name, IntVector3 value);

	boolean setVertexAttrib(String name, IntVector2 value);

	boolean setVertexAttrib(String name, int value);

	boolean setVertexAttrib(int location, DoubleVector4 value);

	boolean setVertexAttrib(int location, DoubleVector3 value);

	boolean setVertexAttrib(int location, DoubleVector2 value);

	boolean setVertexAttrib(int location, double value);

	boolean setVertexAttrib(int location, Vector4 value);

	boolean setVertexAttrib(int location, Vector3 value);

	boolean setVertexAttrib(int location, Vector2 value);

	boolean setVertexAttrib(int location, float value);

	boolean setVertexAttrib(int location, IntVector4 value);

	boolean setVertexAttrib(int location, IntVector3 value);

	boolean setVertexAttrib(int location, IntVector2 value);

	boolean setVertexAttrib(int location, int value);

	boolean addVertexBuffer(int location, VertexBufferType buffer, boolean owned);

	boolean addVertexBuffer(String name, VertexBufferType buffer, boolean owned);

	boolean addVertexBuffer(int location, VertexBufferType buffer);

	boolean addVertexBuffer(String name, VertexBufferType buffer);
	
	Iterable<OpenGLResource> addVertexMesh(String vertexName, String texCoordName, String normalName, VertexMesh mesh);
}
