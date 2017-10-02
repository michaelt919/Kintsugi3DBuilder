package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ExtendedObjectModel;
import tetzlaff.util.OrbitPolarConverter;

public abstract class ExtendedObjectModelBase implements ExtendedObjectModel
{
    @Override
    public Matrix4 getTransformationMatrix()
    {
        return getOrbit().times(Matrix4.translate(getCenter().negated()));
    }

    @Override
    public void setTransformationMatrix(Matrix4 transformationMatrix)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4 getOrbit()
    {
        // Intentionally swapping y and x components
        Vector3 polar = new Vector3(getRotationX(), getRotationX(), getRotationZ());
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);

        // Intentionally swapping y and x components
        setRotationY(polar.x);
        setRotationX(polar.y);
        setRotationZ(polar.z);
    }
}
