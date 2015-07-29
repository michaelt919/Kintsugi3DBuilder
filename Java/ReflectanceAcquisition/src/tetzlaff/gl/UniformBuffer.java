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
	UniformBuffer<ContextType> setData(ByteBuffer data);
	UniformBuffer<ContextType> setData(ByteVertexList data);
	UniformBuffer<ContextType> setData(ShortVertexList data);
	UniformBuffer<ContextType> setData(IntVertexList data);
	UniformBuffer<ContextType> setData(FloatVertexList data);
	UniformBuffer<ContextType> setData(DoubleVertexList data);
}
