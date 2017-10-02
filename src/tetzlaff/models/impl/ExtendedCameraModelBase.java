package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.util.OrbitPolarConverter;

public abstract class ExtendedCameraModelBase implements ExtendedCameraModel
{
    @Override
    public Matrix4 getLookMatrix()
    {
        return Matrix4.lookAt(
            new Vector3(0, 0, getDistance()),
            Vector3.ZERO,
            new Vector3(0, 1, 0)
        ).times(getOrbit().times(
            Matrix4.translate(getTarget().negated())
        ));
    }

    @Override
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4 getOrbit()
    {
        Vector3 polar = new Vector3(getAzimuth(), getInclination(), getTwist());
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
        setAzimuth(polar.x);
        setInclination(polar.y);
        setTwist(polar.z);
    }

    @Override
    public float getDistance()
    {
        return (float) Math.pow(10, getLog10Distance());
    }

    @Override
    public void setDistance(float distance)
    {
        setLog10Distance((float)Math.log10(distance));
    }
}
