package tetzlaff.gl;

public interface Renderable<ProgramType extends Program<?, ? super TextureType>, VertexBufferType extends VertexBuffer, FramebufferType extends Framebuffer, TextureType>
{
	ProgramType program();

	void draw(PrimitiveMode primitiveMode, FramebufferType framebuffer);

	void draw(PrimitiveMode primitiveMode, FramebufferType framebuffer, int x, int y,
			int width, int height);

	void draw(PrimitiveMode primitiveMode, FramebufferType framebuffer, int width,
			int height);

	void draw(PrimitiveMode primitiveMode, int width, int height);
	
	void setVertexAttrib(String name, double value1, double value2,
			double value3, double value4);

	void setVertexAttrib(String name, double value1, double value2,
			double value3);

	void setVertexAttrib(String name, double value1, double value2);

	void setVertexAttrib(String name, double value);

	void setVertexAttrib(String name, float value1, float value2,
			float value3, float value4);

	void setVertexAttrib(String name, float value1, float value2,
			float value3);

	void setVertexAttrib(String name, float value1, float value2);

	void setVertexAttrib(String name, float value);

	void setVertexAttrib(String name, int value1, int value2,
			int value3, int value4);

	void setVertexAttrib(String name, int value1, int value2,
			int value3);

	void setVertexAttrib(String name, int value1, int value2);

	void setVertexAttrib(String name, int value);

	void setVertexAttrib(int location, double value1, double value2,
			double value3, double value4);

	void setVertexAttrib(int location, double value1, double value2,
			double value3);

	void setVertexAttrib(int location, double value1, double value2);

	void setVertexAttrib(int location, double value);

	void setVertexAttrib(int location, float value1, float value2,
			float value3, float value4);

	void setVertexAttrib(int location, float value1, float value2,
			float value3);

	void setVertexAttrib(int location, float value1, float value2);

	void setVertexAttrib(int location, float value);

	void setVertexAttrib(int location, int value1, int value2,
			int value3, int value4);

	void setVertexAttrib(int location, int value1, int value2,
			int value3);

	void setVertexAttrib(int location, int value1, int value2);

	void setVertexAttrib(int location, int value);

	void addVertexBuffer(int location, VertexBufferType buffer);

	void addVertexBuffer(String name, VertexBufferType buffer);
}
