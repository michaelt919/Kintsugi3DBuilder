package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.CameraModel;

public class SimpleCameraModel implements CameraModel
{
    private Matrix4 lookMatrix;

    public SimpleCameraModel()
    {
        this(Matrix4.IDENTITY);
    }

    public SimpleCameraModel(Matrix4 lookMatrix)
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

    @Override
    public void setTarget(Vector3 target)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        throw new UnsupportedOperationException();
    }
}
