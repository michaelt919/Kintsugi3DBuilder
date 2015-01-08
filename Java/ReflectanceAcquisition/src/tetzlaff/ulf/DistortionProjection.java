package tetzlaff.ulf;

import tetzlaff.gl.helpers.Matrix4;

public class DistortionProjection implements Projection
{
    public final float width;
    public final float height;
    public final float fx;
    public final float fy;
    public final float cx;
    public final float cy;
    public final float k1;
    public final float k2;
    public final float k3;
    public final float p1;
    public final float p2;
    
    public DistortionProjection( 
		float width, float height, 
		float fx, float fy, 
		float cx, float cy, 
		float k1, float k2, float k3,
		float p1, float p2)
    {
    	this.width = width;
    	this.height = height;
        this.fx = fx;
        this.fy = fy;
        this.cx = cx;
        this.cy = cy;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.p1 = p1;
        this.p2 = p2;
    }
    
    public DistortionProjection( 
		float width, float height, 
		float fx, float fy, 
		float cx, float cy,
		float k1, float k2, float k3)
	{
    	this(width, height, fx, fy, cx, cy, k1, k2, k3, 0.0f, 0.0f);
	}
    
    public DistortionProjection( 
		float width, float height, 
		float fx, float fy, 
		float cx, float cy)
	{
    	this(width, height, fx, fy, cx, cy, 0.0f, 0.0f, 0.0f);
	}
    
    @Override
    public Matrix4 getProjectionMatrix(float nearPlane, float farPlane)
    {
    	return Matrix4.perspective(2.0f*(float)Math.atan2(height, 2*fy), width/height, nearPlane, farPlane);
    }
}
