package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Matrix4;

public interface ReadonlyExtendedCameraModel 
{
	/**
     * This method is intended to return whether or not the selected camera is locked.
     * It is called by the render side of the program, and when it returns true
     * the camera should not be able to be changed using the tools in the render window.
     * @return true for locked
     */
    public boolean getLocked();

    public Matrix4 getOrbit();
    public float getLog10Distance();
    public float getDistance();
    public float getTwist();
    public float getAzimuth();
    public float getInclination();
}
