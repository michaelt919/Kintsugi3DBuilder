package tetzlaff.ulf;

import tetzlaff.gl.helpers.Matrix4;

public interface Projection 
{
	Matrix4 getProjectionMatrix(float nearPlane, float farPlane);
}
