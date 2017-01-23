package tetzlaff.gl.helpers;

public interface LightController 
{
	int getLightCount();
	Vector3 getLightColor(int i);
	void setLightColor(int i, Vector3 lightColor);
	Matrix4 getLightMatrix(int i);
}
