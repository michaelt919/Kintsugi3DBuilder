package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyLightModel 
{
	int getLightCount();
	boolean isLightVisualizationEnabled(int i);
	
	Vector3 getLightColor(int i);
	Vector3 getAmbientLightColor();
	boolean getEnvironmentMappingEnabled();
	Matrix4 getLightMatrix(int i);
}
