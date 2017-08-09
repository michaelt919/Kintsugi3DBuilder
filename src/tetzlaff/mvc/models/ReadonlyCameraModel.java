package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyCameraModel 
{
	Matrix4 getLookMatrix();
	
	default Vector3 getCenter()
	{
		return Vector3.ZERO;
	}
}
