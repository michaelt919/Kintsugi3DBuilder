package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyEnvironmentMapModel
{
    Vector3 getAmbientLightColor() ;
    boolean getEnvironmentMappingEnabled();
    Matrix4 getEnvironmentMapMatrix();
}
