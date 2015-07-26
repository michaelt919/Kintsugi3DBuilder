package tetzlaff.gl;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

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
