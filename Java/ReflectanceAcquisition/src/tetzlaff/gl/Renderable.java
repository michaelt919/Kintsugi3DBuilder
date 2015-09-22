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
	/**
	 * Gets the shader program that will be used by this renderable object.
	 * @return
	 */
	Program<ContextType> program();

	/**
	 * Draws the renderable to a particular framebuffer using the entire framebuffer as the viewport.
	 * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
	 * @param framebuffer The framebuffer to render to.
	 */
	void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer);

	/**
	 * Draws the renderable to a particular framebuffer within a specified viewport rectangle.
	 * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
	 * @param framebuffer The framebuffer to render to.
	 * @param x The number of pixels to the left edge of the viewport rectangle.
	 * @param y The number of pixels to the bottom edge of the viewport rectangle.
	 * @param width The width of the viewport rectangle in pixels.
	 * @param height The height of the viewport rectangle in pixels.
	 */
	void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer, int x, int y,
			int width, int height);

	/**
	 * Draw the renderable to the default framebuffer using the entire framebuffer as the viewport.
	 * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
	 */
	void draw(PrimitiveMode primitiveMode);
	
	/**
	 *  Draws the renderable to default framebuffer within a specified viewport rectangle.
	 * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
	 * @param x The number of pixels to the left edge of the viewport rectangle.
	 * @param y The number of pixels to the bottom edge of the viewport rectangle.
	 * @param width The width of the viewport rectangle in pixels.
	 * @param height The height of the viewport rectangle in pixels.
	 */
	void draw(PrimitiveMode primitiveMode, int x, int y, int width, int height);
	
	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, DoubleVector4 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, DoubleVector3 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, DoubleVector2 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, double value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, Vector4 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, Vector3 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, Vector2 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, float value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, IntVector4 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, IntVector3 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, IntVector2 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
	 */
	boolean setVertexAttrib(String name, int value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, DoubleVector4 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, DoubleVector3 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, DoubleVector2 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, double value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, Vector4 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, Vector3 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, Vector2 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, float value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, IntVector4 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, IntVector3 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, IntVector2 value);

	/**
	 * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
	 * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
	 * @param location The location of the vertex attribute to set.
	 * @param value The value to set the vertex attribute to.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean setVertexAttrib(int location, int value);

	/**
	 * Designates a vertex buffer to be used for the vertex attribute at a particular location in this renderable's shader program.
	 * @param location The location of the vertex attribute to set.
	 * @param buffer The vertex buffer to bind to the vertex attribute.
	 * @return true if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean addVertexBuffer(int location, VertexBuffer<ContextType> buffer);

	/**
	 * Designates a vertex buffer to be used for the vertex attribute with a particular shader name.
	 * @param name The name used to reference the vertex attribute within the shaders.
	 * @param buffer The vertex buffer to bind to the vertex attribute.
	 * @return if the vertex attribute was successfully set;
	 * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
	 */
	boolean addVertexBuffer(String name, VertexBuffer<ContextType> buffer);
}
