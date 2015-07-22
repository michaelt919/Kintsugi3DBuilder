package tetzlaff.gl;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public interface VertexBuffer<ContextType extends Context> extends Resource
{
	int count();
	void setData(ByteVertexList data, boolean unsigned);
	void setData(ShortVertexList data, boolean unsigned);
	void setData(IntVertexList data, boolean unsigned);
	void setData(FloatVertexList data, boolean normalize);
	void setData(DoubleVertexList data, boolean normalize);
}
