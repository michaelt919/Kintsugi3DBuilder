package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Matrix4;

public class BasicCameraModel implements CameraModel
{
	private Matrix4 lookMatrix;
	
	public BasicCameraModel(Matrix4 lookMatrix)
	{
		this.lookMatrix = lookMatrix;
	}
	
	@Override
	public Matrix4 getLookMatrix() 
	{
		return this.lookMatrix;
	}
	
	@Override
	public void setLookMatrix(Matrix4 lookMatrix) 
	{
		this.lookMatrix = lookMatrix;
	}
}
