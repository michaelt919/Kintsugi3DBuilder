package tetzlaff.models;

import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyLightInstanceModel extends ReadonlyCameraModel
{
    Vector3 getColor();
    boolean isEnabled();
}
