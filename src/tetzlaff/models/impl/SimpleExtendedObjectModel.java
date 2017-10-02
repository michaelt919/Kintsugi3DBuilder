package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Vector3;

public class SimpleExtendedObjectModel extends ExtendedObjectModelBase
{
    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float rotationZ = 0.0f;
    private Vector3 center = Vector3.ZERO;

    @Override
    public boolean isLocked()
    {
        return false;
    }

    @Override
    public float getRotationZ()
    {
        return rotationZ;
    }

    @Override
    public float getRotationY()
    {
        return rotationY;
    }

    @Override
    public float getRotationX()
    {
        return rotationX;
    }

    @Override
    public Vector3 getCenter()
    {
        return center;
    }

    @Override
    public void setCenter(Vector3 center)
    {
        this.center = center;
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        this.rotationZ = rotationZ;
    }

    @Override
    public void setRotationY(float rotationY)
    {
        this.rotationY = rotationY;
    }

    @Override
    public void setRotationX(float rotationX)
    {
        this.rotationX = rotationX;
    }
}
