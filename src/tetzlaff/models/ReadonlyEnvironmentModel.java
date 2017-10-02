package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyEnvironmentModel
{
    double getEnvironmentRotation();
    double getEnvironmentIntensity();

    boolean isEnvironmentMappingEnabled();
    Vector3 getEnvironmentColor();
    Matrix4 getEnvironmentMapMatrix();
}
