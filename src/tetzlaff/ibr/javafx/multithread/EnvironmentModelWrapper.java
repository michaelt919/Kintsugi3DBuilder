package tetzlaff.ibr.javafx.multithread;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.util.MultithreadValue;
import tetzlaff.models.EnvironmentModel;

public class EnvironmentModelWrapper implements EnvironmentModel
{
    private final EnvironmentModel baseModel;
    private final MultithreadValue<Float> environmentRotation;
    private final MultithreadValue<Float> environmentIntensity;

    public EnvironmentModelWrapper(EnvironmentModel baseModel)
    {
        this.baseModel = baseModel;
        this.environmentRotation = MultithreadValue.createFromFunctions(baseModel::getEnvironmentRotation, baseModel::setEnvironmentRotation);
        this.environmentIntensity = MultithreadValue.createFromFunctions(baseModel::getEnvironmentIntensity, baseModel::setEnvironmentIntensity);
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return baseModel.isEnvironmentMappingEnabled();
    }

    @Override
    public Vector3 getEnvironmentColor()
    {
        return baseModel.getEnvironmentColor();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix()
    {
        return baseModel.getEnvironmentMapMatrix();
    }

    @Override
    public float getEnvironmentRotation()
    {
        return this.environmentRotation.getValue();
    }

    @Override
    public float getEnvironmentIntensity()
    {
        return this.environmentIntensity.getValue();
    }

    @Override
    public void setEnvironmentRotation(float environmentRotation)
    {
        this.environmentRotation.setValue(environmentRotation);
    }

    @Override
    public void setEnvironmentIntensity(float environmentIntensity)
    {
        this.environmentIntensity.setValue(environmentIntensity);
    }
}
