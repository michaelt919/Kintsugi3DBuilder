package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyLightingModel 
{
    ReadonlyLightWidgetModel getLightWidgetModel(int index);

    int getLightCount();
    boolean isLightVisualizationEnabled(int index);
    boolean areLightWidgetsEthereal();

    Vector3 getAmbientLightColor();
    boolean isEnvironmentMappingEnabled();
    Matrix4 getEnvironmentMapMatrix();

    Vector3 getLightColor(int i);
    Matrix4 getLightMatrix(int i);
    Vector3 getLightCenter(int i);

    Vector3 getBackgroundColor();
    BackgroundMode getBackgroundMode();
}
