package tetzlaff.ibrelight.javafx.internal;//Created by alexk on 7/21/2017.

import com.sun.istack.internal.NotNull;
import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.models.impl.ExtendedCameraModelBase;

public class CameraModelImpl extends ExtendedCameraModelBase
{
    private ObservableValue<CameraSetting> selectedCameraSetting;
    private final CameraSetting sentinel = new CameraSetting();

    public CameraModelImpl()
    {
        sentinel.setLocked(true);
        sentinel.setName("sentinel");
    }

    public void setSelectedCameraSetting(ObservableValue<CameraSetting> selectedCameraSetting)
    {
        this.selectedCameraSetting = selectedCameraSetting;
    }

    @NotNull
    private CameraSetting getActiveCameraSetting()
    {
        if (selectedCameraSetting == null || selectedCameraSetting.getValue() == null)
        {
            return sentinel;
        }
        else
        {
            return selectedCameraSetting.getValue();
        }
    }

    @Override
    public float getLog10Distance()
    {
        return (float) getActiveCameraSetting().getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setLog10Distance(log10Distance);
        }
    }

    @Override
    public Vector3 getTarget()
    {
        return new Vector3((float) getActiveCameraSetting().getXCenter(),
            (float) getActiveCameraSetting().getYCenter(),
            (float) getActiveCameraSetting().getZCenter());
    }

    @Override
    public void setTarget(Vector3 target)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setXCenter(target.x);
            getActiveCameraSetting().setYCenter(target.y);
            getActiveCameraSetting().setZCenter(target.z);
        }
    }

    @Override
    public float getTwist()
    {
        return (float) getActiveCameraSetting().getTwist();
    }

    @Override
    public void setTwist(float twist)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setTwist(twist);
        }
    }

    @Override
    public float getAzimuth()
    {
        return (float) getActiveCameraSetting().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setAzimuth(azimuth);
        }
    }

    @Override
    public float getInclination()
    {
        return (float) getActiveCameraSetting().getInclination();
    }

    @Override
    public void setInclination(float inclination)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setInclination(inclination);
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
        return getActiveCameraSetting().isLocked();
    }

    @Override
    public float getHorizontalFOV()
    {
        return (float)(getActiveCameraSetting().getFOV() * Math.PI / 180);
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setFOV(fov * 180 / Math.PI);
        }
    }

    @Override
    public float getFocalLength()
    {
        return (float)getActiveCameraSetting().getFocalLength();
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            getActiveCameraSetting().setFocalLength(focalLength);
        }
    }

    @Override
    public boolean isOrthographic()
    {
        return getActiveCameraSetting().isOrthographic();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        if (!getActiveCameraSetting().isLocked())
        {
            this.getActiveCameraSetting().setOrthographic(orthographic);
        }
    }
}
