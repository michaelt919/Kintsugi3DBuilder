package tetzlaff.models;

import tetzlaff.gl.vecmath.Vector3;

public interface LightPrototypeModel extends ReadonlyLightPrototypeModel
{
    void setColor(Vector3 color);
    void setSpotSize(float spotSize);
    void setSpotTaper(float spotTaper);
}
