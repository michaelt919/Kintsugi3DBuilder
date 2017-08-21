package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface EnvironmentMapModel extends ReadonlyEnvironmentMapModel
{
    void setAmbientLightColor(Vector3 ambientLightColor);
    void setEnvironmentMappingEnabled(boolean enabled);
    void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix);
}
