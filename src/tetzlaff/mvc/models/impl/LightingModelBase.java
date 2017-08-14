package tetzlaff.mvc.models.impl;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.LightInstanceModel;
import tetzlaff.mvc.models.ReadonlyLightingModel;

public abstract class LightingModelBase implements ReadonlyLightingModel 
{
    public abstract LightInstanceModel getLightInstanceModel(int i);
    public abstract void setLightColor(int i, Vector3 color);

    private EnvironmentMapModelBase environmentMapModel;

    public LightingModelBase(EnvironmentMapModelBase ev) 
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
