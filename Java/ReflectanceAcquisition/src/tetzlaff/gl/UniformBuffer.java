package tetzlaff.gl;

import java.nio.ByteBuffer;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

/**
 * An interface for a uniform buffer object that can provide data to be used in conjunction with a shader program.
 * A uniform buffer should contain data that does not vary between primitives in a single draw call.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface UniformBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	/**
	 * Sets the content of the uniform buffer.
	 * @param data A raw byte buffer containing the data.
	 * @return The calling object.
	 */
	UniformBuffer<ContextType> setData(ByteBuffer data);
	
	/**
	 * Sets the content of the uniform buffer.
	 * @param data A list of the values to be stored as 8-bit integers.
	 * @return The calling object.
	 */
	UniformBuffer<ContextType> setData(ByteVertexList data);
	
	/**
	 * Sets the content of the uniform buffer.
	 * @param data A list of the values to be stored as 16-bit integers.
	 * @return The calling object.
	 */
	UniformBuffer<ContextType> setData(ShortVertexList data);
	
	/**
	 * Sets the content of the uniform buffer.
	 * @param data A list of the values to be stored as 32-bit integers.
	 * @return The calling object.
	 */
	UniformBuffer<ContextType> setData(IntVertexList data);
	
	/**
	 * Sets the content of the uniform buffer.
	 * @param data A list of the values to be stored as 32-bit floating-point numbers.
	 * @return The calling object.
	 */
	UniformBuffer<ContextType> setData(FloatVertexList data);
	
	/**
	 * Sets the content of the uniform buffer.
	 * @param data A list of the values to be stored as 64-bit floating-point numbers.
	 * @return The calling object.
	 */
	UniformBuffer<ContextType> setData(DoubleVertexList data);
}
