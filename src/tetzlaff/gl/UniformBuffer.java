package tetzlaff.gl;

import java.nio.ByteBuffer;

import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

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
    UniformBuffer<ContextType> setData(NativeVectorBuffer data);
}
