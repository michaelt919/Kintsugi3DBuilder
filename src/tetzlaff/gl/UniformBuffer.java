package tetzlaff.gl;

import java.nio.ByteBuffer;

import tetzlaff.gl.nativelist.NativeByteVectorList;
import tetzlaff.gl.nativelist.NativeDoubleVectorList;
import tetzlaff.gl.nativelist.NativeFloatVectorList;
import tetzlaff.gl.nativelist.NativeIntVectorList;
import tetzlaff.gl.nativelist.NativeShortVectorList;

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
	UniformBuffer<ContextType> setData(NativeByteVectorList data);
	UniformBuffer<ContextType> setData(NativeShortVectorList data);
	UniformBuffer<ContextType> setData(NativeIntVectorList data);
	UniformBuffer<ContextType> setData(NativeFloatVectorList data);
	UniformBuffer<ContextType> setData(NativeDoubleVectorList data);
}
