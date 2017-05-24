package tetzlaff.mvc.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.mvc.models.CameraModel;

public class BasicCameraModel implements CameraModel
{
	private Matrix4 lookMatrix;
	
	public BasicCameraModel()
	{
		this(Matrix4.identity());
	}
	
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
