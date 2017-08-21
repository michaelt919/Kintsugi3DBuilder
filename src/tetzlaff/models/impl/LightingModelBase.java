package tetzlaff.models.impl;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.EnvironmentMapModel;
import tetzlaff.models.ExtendedLightingModel;
public abstract class LightingModelBase implements ExtendedLightingModel
{
    private final EnvironmentMapModel environmentMapModel;

    protected LightingModelBase(EnvironmentMapModel environmentMapModel)
    {
        this.environmentMapModel = environmentMapModel;
    }

    @Override
    public final Vector3 getAmbientLightColor() 
    {
        return environmentMapModel.getAmbientLightColor();
    }

    @Override
    public final void setAmbientLightColor(Vector3 ambientLightColor)
    {
        environmentMapModel.setAmbientLightColor(ambientLightColor);
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return environmentMapModel.isEnvironmentMappingEnabled();
    }

    @Override
    public void setEnvironmentMappingEnabled(boolean enabled)
    {
        environmentMapModel.setEnvironmentMappingEnabled(enabled);
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() 
    {
        return environmentMapModel.getEnvironmentMapMatrix();
    }

    @Override
    public void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix)
    {
        environmentMapModel.setEnvironmentMapMatrix(environmentMapMatrix);
    }
}
