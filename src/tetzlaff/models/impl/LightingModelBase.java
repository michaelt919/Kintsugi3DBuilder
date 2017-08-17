package tetzlaff.models.impl;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;

public abstract class LightingModelBase implements ReadonlyLightingModel 
{
    private final ReadonlyEnvironmentMapModel environmentMapModel;

    public LightingModelBase(ReadonlyEnvironmentMapModel environmentMapModel) 
    {
        this.environmentMapModel = environmentMapModel;
    }

    @Override
    public final Vector3 getAmbientLightColor() 
    {
        return environmentMapModel.getAmbientLightColor();
    }

    @Override
    public boolean getEnvironmentMappingEnabled() 
    {
        return environmentMapModel.getEnvironmentMappingEnabled();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() 
    {
        return environmentMapModel.getEnvironmentMapMatrix();
    }
}
