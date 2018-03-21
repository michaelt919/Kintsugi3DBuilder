package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface LightingModel extends ReadonlyLightingModel
{
    @Override
    LightWidgetModel getLightWidgetModel(int index);

    @Override
    LightPrototypeModel getLightPrototype(int i);

    void setLightWidgetsEthereal(boolean lightWidgetsEthereal);

    void setAmbientLightColor(Vector3 ambientLightColor);
    void setEnvironmentMappingEnabled(boolean enabled);
    void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix);

    void setLightMatrix(int i, Matrix4 lightMatrix);
    void setLightCenter(int i, Vector3 lightCenter);

    void setBackgroundColor(Vector3 backgroundColor);
    void setBackgroundMode(BackgroundMode backgroundMode);
}
