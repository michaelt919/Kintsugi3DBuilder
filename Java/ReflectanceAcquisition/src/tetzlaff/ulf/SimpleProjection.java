package tetzlaff.ulf;

import tetzlaff.gl.helpers.Matrix4;

public class SimpleProjection implements Projection
{
	public final float aspectRatio;
	public final float verticalFieldOfView;
	
	public SimpleProjection(float aspectRatio, float verticalFieldOfView) 
	{
		this.aspectRatio = aspectRatio;
		this.verticalFieldOfView = verticalFieldOfView;
	}

	@Override
	public Matrix4 getProjectionMatrix(float nearPlane, float farPlane) 
	{
		return Matrix4.perspective(this.verticalFieldOfView, this.aspectRatio, nearPlane, farPlane);
	}

}
