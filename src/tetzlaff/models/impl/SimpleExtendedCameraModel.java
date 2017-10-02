package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Vector3;

public class SimpleExtendedCameraModel extends ExtendedCameraModelBase
{
    private Vector3 target = Vector3.ZERO;
    private float azimuth = 0.0f;
    private float inclination = 0.0f;
    private float log10Distance = 0.0f;
    private float twist = 0.0f;
    private float horizontalFOV = 90.0f;
    private float focalLength = 18.0f;
    private boolean orthographic = false;

    @Override
    public boolean isLocked()
    {
        return false;
    }

    @Override
    public Vector3 getTarget()
    {
        return target;
    }

    @Override
    public void setTarget(Vector3 target)
    {
        this.target = target;
    }

    @Override
    public float getAzimuth()
    {
        return azimuth;
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        this.azimuth = azimuth;
    }

    @Override
    public float getInclination()
    {
        return inclination;
    }

    @Override
    public void setInclination(float inclination)
    {
        this.inclination = inclination;
    }

    @Override
    public float getLog10Distance()
    {
        return log10Distance;
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        this.log10Distance = log10Distance;
    }

    @Override
    public float getTwist()
    {
        return twist;
    }

    @Override
    public void setTwist(float twist)
    {
        this.twist = twist;
    }

    @Override
    public float getHorizontalFOV()
    {
        return horizontalFOV;
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        this.horizontalFOV = fov;
    }

    @Override
    public float getFocalLength()
    {
        return focalLength;
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        this.focalLength = focalLength;
    }

    @Override
    public boolean isOrthographic()
    {
        return orthographic;
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        this.orthographic = orthographic;
    }
}
