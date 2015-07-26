package tetzlaff.gl;

import java.nio.ByteBuffer;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public interface UniformBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	UniformBuffer<ContextType> setData(ByteBuffer data);
	UniformBuffer<ContextType> setData(ByteVertexList data);
	UniformBuffer<ContextType> setData(ShortVertexList data);
	UniformBuffer<ContextType> setData(IntVertexList data);
	UniformBuffer<ContextType> setData(FloatVertexList data);
	UniformBuffer<ContextType> setData(DoubleVertexList data);
}
