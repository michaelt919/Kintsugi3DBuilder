package tetzlaff.gl;

import tetzlaff.gl.helpers.DoubleVector2;
import tetzlaff.gl.helpers.DoubleVector3;
import tetzlaff.gl.helpers.DoubleVector4;
import tetzlaff.gl.helpers.IntVector2;
import tetzlaff.gl.helpers.IntVector3;
import tetzlaff.gl.helpers.IntVector4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;

/**
 * A renderable entity for a GL context.
 * A "renderable" is essentially a tuple consisting of a program (the instructions) and a set of vertex buffers (the data).
 * It is in some ways analogous to an OpenGL vertex array object (VAO), but it is tied to a specific program 
 * since there is no guarantee that different programs will expect the same set of vertex attributes.
 * Unlike many other GL entities, a Renderable is not a proper "resource"; any graphics resources that
 * a Renderable does allocate will be automatically deleted when the entity is garbage collected by the JRE.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the Renderable is associated with.
 */
public interface Renderable<ContextType extends Context<ContextType>> extends Contextual<ContextType>
{
	Program<ContextType> program();

	void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer);

	void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer, int x, int y,
			int width, int height);

	void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer, int width,
			int height);
	
	void draw(PrimitiveMode primitiveMode, ContextType context);
	
	void draw(PrimitiveMode primitiveMode, ContextType context, int x, int y, int width, int height);

	void draw(PrimitiveMode primitiveMode, ContextType context, int width, int height);
	
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

	boolean addVertexBuffer(int location, VertexBuffer<ContextType> buffer);

	boolean addVertexBuffer(String name, VertexBuffer<ContextType> buffer);
}
