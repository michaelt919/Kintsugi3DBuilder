package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.BackgroundMode;
import tetzlaff.models.EnvironmentModel;

public class EnvironmentModelWrapper implements EnvironmentModel
{
    private final EnvironmentModel baseModel;
    private final MultithreadValue<Float> environmentRotation;
    private final MultithreadValue<Float> environmentIntensity;
    private final MultithreadValue<Float> backgroundIntensity;

    public EnvironmentModelWrapper(EnvironmentModel baseModel)
    {
        this.baseModel = baseModel;
        this.environmentRotation = MultithreadValue.createFromFunctions(baseModel::getEnvironmentRotation, baseModel::setEnvironmentRotation);
        this.environmentIntensity = MultithreadValue.createFromFunctions(baseModel::getEnvironmentIntensity, baseModel::setEnvironmentIntensity);
        this.backgroundIntensity = MultithreadValue.createFromFunctions(baseModel::getBackgroundIntensity, baseModel::setBackgroundIntensity);
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
    public float getEnvironmentMapFilteringBias()
    {
        return baseModel.getEnvironmentMapFilteringBias();
    }

    @Override
    public BackgroundMode getBackgroundMode()
    {
        return baseModel.getBackgroundMode();
    }

    @Override
    public Vector3 getBackgroundColor()
    {
        return baseModel.getBackgroundColor();
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
    public float getBackgroundIntensity()
    {
        return this.backgroundIntensity.getValue();
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

    @Override
    public void setBackgroundIntensity(float backgroundIntensity)
    {
        this.backgroundIntensity.setValue(backgroundIntensity);
    }
}
