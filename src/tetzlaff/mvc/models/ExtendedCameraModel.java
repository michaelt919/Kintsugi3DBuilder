package tetzlaff.mvc.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ExtendedCameraModel extends CameraModel, ReadonlyExtendedCameraModel 
{
    public void setOrbit(Matrix4 orbit);
    public void setLog10Distance(float log10Distance);
    public void setDistance(float distance);
    public void setCenter(Vector3 offSet);
    public void setTwist(float twist);
    public void setAzimuth(float azmuth);
    public void setInclination(float inclination);
}
