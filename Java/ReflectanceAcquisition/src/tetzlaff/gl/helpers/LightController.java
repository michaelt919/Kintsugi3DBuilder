package tetzlaff.gl.helpers;

public interface LightController 
{
	int getLightCount();
	Vector3 getLightColor(int i);
	void setLightColor(int i, Vector3 lightColor);
	Vector3 getAmbientLightColor();
	void setAmbientLightColor(Vector3 ambientLightColor);
	boolean getEnvironmentMappingEnabled();
	void setEnvironmentMappingEnabled(boolean enabled);
	Matrix4 getLightMatrix(int i);
}
