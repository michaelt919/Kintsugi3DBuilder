package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.ExtendedObjectModel;
import tetzlaff.models.impl.ExtendedObjectModelBase;

public class ObjectModelWrapper extends ExtendedObjectModelBase
{
    private final ExtendedObjectModel baseModel;
    private final MultithreadValue<Vector3> center;
    private final MultithreadValue<Float> rotationX;
    private final MultithreadValue<Float> rotationY;
    private final MultithreadValue<Float> rotationZ;

    public ObjectModelWrapper(ExtendedObjectModel baseModel)
    {
        this.baseModel = baseModel;
        this.center = MultithreadValue.createFromFunctions(baseModel::getCenter, baseModel::setCenter);
        this.rotationX = MultithreadValue.createFromFunctions(baseModel::getRotationX, baseModel::setRotationX);
        this.rotationY = MultithreadValue.createFromFunctions(baseModel::getRotationY, baseModel::setRotationY);
        this.rotationZ = MultithreadValue.createFromFunctions(baseModel::getRotationZ, baseModel::setRotationZ);
    }

    @Override
    public boolean isLocked()
    {
        return baseModel.isLocked();
    }

    @Override
    public Vector3 getCenter()
    {
        return this.center.getValue();
    }

    @Override
    public float getRotationZ()
    {
        return this.rotationZ.getValue();
    }

    @Override
    public float getRotationY()
    {
        return this.rotationY.getValue();
    }

    @Override
    public float getRotationX()
    {
        return this.rotationX.getValue();
    }

    @Override
    public void setCenter(Vector3 center)
    {
        this.center.setValue(center);
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        this.rotationZ.setValue(rotationZ);
    }

    @Override
    public void setRotationY(float rotationY)
    {
        this.rotationY.setValue(rotationY);
    }

    @Override
    public void setRotationX(float rotationX)
    {
        this.rotationX.setValue(rotationX);
    }
}
