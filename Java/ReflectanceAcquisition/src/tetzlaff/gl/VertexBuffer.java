package tetzlaff.gl;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

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
	VertexBuffer<ContextType> setData(ByteVertexList data, boolean unsigned);
	VertexBuffer<ContextType> setData(ShortVertexList data, boolean unsigned);
	VertexBuffer<ContextType> setData(IntVertexList data, boolean unsigned);
	VertexBuffer<ContextType> setData(FloatVertexList data, boolean normalize);
	VertexBuffer<ContextType> setData(DoubleVertexList data, boolean normalize);
	
	default VertexBuffer<ContextType> setData(ByteVertexList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(ShortVertexList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(IntVertexList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(FloatVertexList data)
	{
		return this.setData(data, false);
	}
	
	default VertexBuffer<ContextType> setData(DoubleVertexList data)
	{
		return this.setData(data, false);
	}
}
