package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;

public interface ObjectModel extends ReadonlyObjectModel
{
	void setTransformationMatrix(Matrix4 transformationMatrix);
}
