package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyEnvironmentModel
{
    float getEnvironmentRotation();
    float getEnvironmentIntensity();

    boolean isEnvironmentMappingEnabled();
    Vector3 getEnvironmentColor();
    Matrix4 getEnvironmentMapMatrix();
    float getEnvironmentMapFilteringBias();

    BackgroundMode getBackgroundMode();
    float getBackgroundIntensity();
    Vector3 getBackgroundColor();
}
