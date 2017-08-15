package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface LightingModel extends ReadonlyLightingModel
{
	void setLightColor(int i, Vector3 lightColor);
	void setAmbientLightColor(Vector3 ambientLightColor);
	void setEnvironmentMappingEnabled(boolean enabled);
	void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix);
	void setLightMatrix(int i, Matrix4 lightMatrix);
	void setLightCenter(int i, Vector3 lightTargetPoint);
}
