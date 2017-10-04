package tetzlaff.ibrelight.core;

import tetzlaff.gl.vecmath.Matrix4;

/**
 * Creates a perspective projection that also maintains camera distortion parameters (for Brown's distortion model).
 * These parameters are maintained for reference only; they do not actually affect the projection matrix.
 * The projection matrix is computed using nothing but the sensor width and height and the parameter fy.
 * @author Michael Tetzlaff
 *
 */
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
    public final float k4;
    public final float p1;
    public final float p2;
    public final float skew;
    
    /**
     * Creates a new distortion projection.
     * @param width The sensor width in some arbitrary units.
     * @param height The sensor height in some arbitrary units.
     * @param fx The "horizontal" focal distance in the same units as the sensor dimensions.
     * It is expected that this parameter will be the same as fy after lens distortion is corrected and is therefore ignored when computing a projection matrix.
     * @param fy The "vertical" focal distance in the same units as the sensor dimensions - this is the parameter that actually determines the field-of-view of the projection matrix.
     * @param cx The horizontal center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param cy The vertical center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param k1 The "k1" camera distortion parameter.
     * @param k2 The "k2" camera distortion parameter.
     * @param k3 The "k3" camera distortion parameter.
     * @param p1 The "p1" camera distortion parameter.
     * @param p2 The "p2" camera distortion parameter.
     */
    public DistortionProjection( 
        float width, float height,
        float fx, float fy,
        float cx, float cy,
        float k1, float k2, float k3, float k4,
        float p1, float p2, float skew)
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
        this.k4 = k4;
        this.p1 = p1;
        this.p2 = p2;
        this.skew = skew;
    }
    
    /**
     * Creates a new distortion projection.  p1 and p2 are assumed to be zero.
     * @param width The sensor width in some arbitrary units.
     * @param height The sensor height in some arbitrary units.
     * @param fx The "horizontal" focal distance in the same units as the sensor dimensions.
     * It is expected that this parameter will be the same as fy after lens distortion is corrected and is therefore ignored when computing a projection matrix.
     * @param fy The "vertical" focal distance in the same units as the sensor dimensions - this is the parameter that actually determines the field-of-view of the projection matrix.
     * @param cx The horizontal center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param cy The vertical center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param k1 The "k1" camera distortion parameter.
     * @param k2 The "k2" camera distortion parameter.
     * @param k3 The "k3" camera distortion parameter.
     * @param k4 The "k4" camera distortion parameter.
     */
    public DistortionProjection( 
        float width, float height,
        float fx, float fy,
        float cx, float cy,
        float k1, float k2, float k3, float k4)
    {
        this(width, height, fx, fy, cx, cy, k1, k2, k3, k4, 0.0f, 0.0f, 0.0f);
    }
    
    /**
     * Creates a new distortion projection.  k4, p1 and p2 are assumed to be zero.
     * @param width The sensor width in some arbitrary units.
     * @param height The sensor height in some arbitrary units.
     * @param fx The "horizontal" focal distance in the same units as the sensor dimensions.
     * It is expected that this parameter will be the same as fy after lens distortion is corrected and is therefore ignored when computing a projection matrix.
     * @param fy The "vertical" focal distance in the same units as the sensor dimensions - this is the parameter that actually determines the field-of-view of the projection matrix.
     * @param cx The horizontal center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param cy The vertical center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param k1 The "k1" camera distortion parameter.
     * @param k2 The "k2" camera distortion parameter.
     * @param k3 The "k3" camera distortion parameter.
     */
    public DistortionProjection( 
        float width, float height,
        float fx, float fy,
        float cx, float cy,
        float k1, float k2, float k3)
    {
        this(width, height, fx, fy, cx, cy, k1, k2, k3, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Creates a new distortion projection.  k1, k2, k3, k4, p1, and p2 are assumed to be zero.
     * @param width The sensor width in some arbitrary units.
     * @param height The sensor height in some arbitrary units.
     * @param fx The "horizontal" focal distance in the same units as the sensor dimensions.
     * It is expected that this parameter will be the same as fy after lens distortion is corrected and is therefore ignored when computing a projection matrix.
     * @param fy The "vertical" focal distance in the same units as the sensor dimensions - this is the parameter that actually determines the field-of-view of the projection matrix.
     * @param cx The horizontal center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     * @param cy The vertical center of projection.
     * This parameter is for reference only and is not actually used when computing a projection matrix.
     */
    public DistortionProjection( 
        float width, float height,
        float fx, float fy,
        float cx, float cy)
    {
        this(width, height, fx, fy, cx, cy, 0.0f, 0.0f, 0.0f);
    }
    
    @Override
    public float getAspectRatio()
    {
        return width / height;
    }
    
    @Override
    public float getVerticalFieldOfView()
    {
        return 2.0f*(float)Math.atan2(height, 2*fy);
    }
    
    @Override
    public Matrix4 getProjectionMatrix(float nearPlane, float farPlane)
    {
        return Matrix4.perspective(this.getVerticalFieldOfView(), this.getAspectRatio(), nearPlane, farPlane);
    }
    
    @Override
    public String toVSETString()
    {
        return String.format("D\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f",
                                cx, cy, width/height, fx, width, k1, k2, k3, p1, p2);
    }
}
