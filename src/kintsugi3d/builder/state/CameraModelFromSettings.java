package kintsugi3d.builder.state;

import kintsugi3d.builder.state.project.CameraSettings;
import kintsugi3d.gl.vecmath.Vector3;

public abstract class CameraModelFromSettings extends ViewpointModelBase
{
    protected abstract CameraSettings getCameraSettings();

    @Override
    public float getLog10Distance()
    {
        return (float) getCameraSettings().getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setLog10Distance(log10Distance);
        }
    }

    @Override
    public Vector3 getTarget()
    {
        return new Vector3((float) getCameraSettings().getXCenter(),
            (float) getCameraSettings().getYCenter(),
            (float) getCameraSettings().getZCenter());
    }

    @Override
    public void setTarget(Vector3 target)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setXCenter(target.x);
            getCameraSettings().setYCenter(target.y);
            getCameraSettings().setZCenter(target.z);
        }
    }

    @Override
    public float getTwist()
    {
        return (float) getCameraSettings().getTwist();
    }

    @Override
    public void setTwist(float twist)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setTwist(twist);
        }
    }

    @Override
    public float getAzimuth()
    {
        return (float) getCameraSettings().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setAzimuth(azimuth);
        }
    }

    @Override
    public float getInclination()
    {
        return (float) getCameraSettings().getInclination();
    }

    @Override
    public void setInclination(float inclination)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setInclination(inclination);
        }
    }

    /**
     * this method is intended to return whether or not the selected camera is locked.
     * It is called by the render side of the program, and when it returns true
     * the camera should not be able to be changed using the tools in the render window.
     *
     * @return true for locked
     */
    @Override
    public boolean isLocked()
    {
        return getCameraSettings().isLocked();
    }

    @Override
    public float getHorizontalFOV()
    {
        return (float) (getCameraSettings().getFOV() * Math.PI / 180);
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setFOV(fov * 180 / Math.PI);
        }
    }

    @Override
    public float getFocalLength()
    {
        return (float) getCameraSettings().getFocalLength();
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        if (!getCameraSettings().isLocked())
        {
            getCameraSettings().setFocalLength(focalLength);
        }
    }

    @Override
    public boolean isOrthographic()
    {
        return getCameraSettings().isOrthographic();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        if (!getCameraSettings().isLocked())
        {
            this.getCameraSettings().setOrthographic(orthographic);
        }
    }
}
