package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;

public interface ExtendedCameraModel extends CameraModel, ReadonlyExtendedCameraModel 
{
    @Override
    float getHorizontalFOV();

    void setOrbit(Matrix4 orbit);
    void setLog10Distance(float log10Distance);
    void setDistance(float distance);
    void setTwist(float twist);
    void setAzimuth(float azimuth);
    void setInclination(float inclination);
    void setFocalLength(float focalLength);
}
