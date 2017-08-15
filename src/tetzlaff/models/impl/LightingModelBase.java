package tetzlaff.models.impl;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;

public abstract class LightingModelBase implements ReadonlyLightingModel 
{
    private ReadonlyEnvironmentMapModel environmentMapModel;

    public LightingModelBase(ReadonlyEnvironmentMapModel ev) 
    {
        this.environmentMapModel = ev;
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
