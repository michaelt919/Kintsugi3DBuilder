package tetzlaff.models;

import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyLightPrototypeModel
{
    Vector3 getColor();
    float getSpotSize();
    float getSpotTaper();
}
