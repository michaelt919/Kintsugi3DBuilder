package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ExtendedCameraModel extends CameraModel, ReadonlyExtendedCameraModel 
{
    void setOrbit(Matrix4 orbit);
    void setLog10Distance(float log10Distance);
    void setDistance(float distance);
    void setCenter(Vector3 offSet);
    void setTwist(float twist);
    void setAzimuth(float azmuth);
    void setInclination(float inclination);
}
