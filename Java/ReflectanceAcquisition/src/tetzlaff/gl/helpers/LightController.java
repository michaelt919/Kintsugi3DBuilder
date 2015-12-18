package tetzlaff.gl.helpers;

public interface LightController 
{
	int getLightCount();
	Vector3 getLightColor(int i);
	Matrix4 getLightMatrix(int i);
}
