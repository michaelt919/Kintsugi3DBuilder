package tetzlaff.gl;

import java.nio.ByteBuffer;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public interface UniformBuffer<ContextType extends Context> extends Resource 
{
	void setData(ByteBuffer data);
	void setData(ByteVertexList data);
	void setData(ShortVertexList data);
	void setData(IntVertexList data);
	void setData(FloatVertexList data);
	void setData(DoubleVertexList data);
}
