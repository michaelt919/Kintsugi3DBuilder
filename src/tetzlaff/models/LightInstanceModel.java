package tetzlaff.models;//Created by alexk on 7/25/2017.

import tetzlaff.gl.vecmath.Vector3;

public interface LightInstanceModel extends ExtendedCameraModel 
{
    Vector3 getColor();
    void setColor(Vector3 color);
    boolean exists();
}
