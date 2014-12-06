package tetzlaff.gl;

import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;
import tetzlaff.gl.opengl.OpenGLResource;

public interface VertexBuffer extends OpenGLResource
{
	int count();
	void setData(ByteVertexList data, boolean unsigned);
	void setData(ShortVertexList data, boolean unsigned);
	void setData(IntVertexList data, boolean unsigned);
	void setData(FloatVertexList data, boolean normalize);
	void setData(DoubleVertexList data, boolean normalize);
}
