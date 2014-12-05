package openGL.wrappers.interfaces;

import openGL.helpers.ByteVertexList;
import openGL.helpers.DoubleVertexList;
import openGL.helpers.FloatVertexList;
import openGL.helpers.IntVertexList;
import openGL.helpers.ShortVertexList;

public interface VertexBuffer extends GLResource
{
	void useAsVertexAttribute(int attribIndex);
	int count();
	void setData(ByteVertexList data, boolean unsigned);
	void setData(ShortVertexList data, boolean unsigned);
	void setData(IntVertexList data, boolean unsigned);
	void setData(FloatVertexList data, boolean normalize);
	void setData(DoubleVertexList data, boolean normalize);
}
