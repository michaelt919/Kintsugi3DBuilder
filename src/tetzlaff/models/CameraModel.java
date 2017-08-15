package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;

public interface CameraModel extends ReadonlyCameraModel
{
	void setLookMatrix(Matrix4 lookMatrix);
}
