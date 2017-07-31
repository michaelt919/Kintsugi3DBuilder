package tetzlaff.mvc.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ControllableCameraModel extends ReadonlyCameraModel {

    /**
     * this method is intended to return whether or not the selected camera is locked.
     * It is called by the render side of the program, and when it returns true
     * the camera should not be able to be changed using the tools in the render window.
     * @return true for locked
     */
    public boolean getLocked();

    public Matrix4 getOrbit();
    public void setOrbit(Matrix4 orbit);

    public Float getLog10distance();
    public void setLog10distance(Float log10distance);
    public Float getDistance();
    public void setDistance(Float distance);

    public Vector3 getCenter();
    public void setCenter(Vector3 offSet);

    public Double getTwist();
    public void setTwist(Double twist);

    public Double getAzimuth();
    public void setAzimuth(Double azmuth);

    public Double getInclination();
    public void setInclination(Double inclination);



}
