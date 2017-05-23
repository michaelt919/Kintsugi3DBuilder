package tetzlaff.gl;

import tetzlaff.gl.nativelist.NativeByteVectorList;
import tetzlaff.gl.nativelist.NativeDoubleVectorList;
import tetzlaff.gl.nativelist.NativeFloatVectorList;
import tetzlaff.gl.nativelist.NativeIntVectorList;
import tetzlaff.gl.nativelist.NativeShortVectorList;

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
	VertexBuffer<ContextType> setData(NativeByteVectorList data, boolean unsigned);
	VertexBuffer<ContextType> setData(NativeShortVectorList data, boolean unsigned);
	VertexBuffer<ContextType> setData(NativeIntVectorList data, boolean unsigned);
	VertexBuffer<ContextType> setData(NativeFloatVectorList data, boolean normalize);
	VertexBuffer<ContextType> setData(NativeDoubleVectorList data, boolean normalize);
	
	default VertexBuffer<ContextType> setData(NativeByteVectorList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(NativeShortVectorList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(NativeIntVectorList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(NativeFloatVectorList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(NativeDoubleVectorList data)
	{
		return this.setData(data, false);
	}
}
