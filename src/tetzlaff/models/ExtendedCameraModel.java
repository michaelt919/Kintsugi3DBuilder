package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ExtendedCameraModel extends CameraModel, ReadonlyExtendedCameraModel 
{
    @Override
    float getHorizontalFOV();

    void setOrbit(Matrix4 orbit);
    void setLog10Distance(float log10Distance);
    void setDistance(float distance);
    void setTarget(Vector3 target);
    void setTwist(float twist);
    void setAzimuth(float azimuth);
    void setInclination(float inclination);
    void setHorizontalFOV(float fov);
    void setFocalLength(float focalLength);
}
