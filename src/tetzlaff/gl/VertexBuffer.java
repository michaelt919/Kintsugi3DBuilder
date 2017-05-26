package tetzlaff.gl;

import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

/**
 * An interface for a vertex buffer object that can provide data to be used for rendering.
 * A vertex buffer should a series of vertex attributes that can be organized into "primitives" for rendering.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface VertexBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	int count();
	VertexBuffer<ContextType> setData(NativeVectorBuffer data, boolean normalize);
	
	default VertexBuffer<ContextType> setData(NativeVectorBuffer  data)
	{
		return this.setData(data, false);
	}
}
