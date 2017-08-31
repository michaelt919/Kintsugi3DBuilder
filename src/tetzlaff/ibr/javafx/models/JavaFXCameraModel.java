package tetzlaff.ibr.javafx.models;//Created by alexk on 7/21/2017.

import com.sun.istack.internal.NotNull;
import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.util.OrbitPolarConverter;

public class JavaFXCameraModel implements ExtendedCameraModel
{

    private ObservableValue<CameraSetting> selected;
    private final CameraSetting backup = new CameraSetting(
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        1.0,
        0.0,
        90.0,
        18.0,
        true,
        false,
        "backup"
    );

    public void setSelectedCameraSettingProperty(ObservableValue<CameraSetting> selectedCameraSettingProperty)
    {
        this.selected = selectedCameraSettingProperty;
    }

    @NotNull
    private CameraSetting getActiveCameraSetting()
    {
        if (selected == null || selected.getValue() == null)
        {
            return backup;
        }
        else
        {
            return selected.getValue();
        }
    }

    private Matrix4 orbitCache;
    private boolean fromRender = false;

    @Override
    public Matrix4 getLookMatrix()
    {
        return Matrix4.lookAt(
            new Vector3(0, 0, getDistance()),
            Vector3.ZERO,
            new Vector3(0, 1, 0)
        ).times(getOrbit().times(
            Matrix4.translate(getTarget().negated())
        ));
    }

    @Override
    public Matrix4 getOrbit()
    {
        if (fromRender)
        {
            fromRender = false;
            return orbitCache;
        }
        Vector3 poler = new Vector3((float) getActiveCameraSetting().getAzimuth(), (float) getActiveCameraSetting().getInclination(), (float) getActiveCameraSetting().getTwist());
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(poler);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 poler = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
        getActiveCameraSetting().setAzimuth(poler.x);
        getActiveCameraSetting().setInclination(poler.y);
        getActiveCameraSetting().setTwist(poler.z);
        orbitCache = orbit;
        fromRender = true;
    }

    @Override
    public float getLog10Distance()
    {
        return (float) getActiveCameraSetting().getLog10distance();
    }

    @Override
    public void setLog10Distance(float log10distance)
    {
        getActiveCameraSetting().setLog10distance(log10distance);
    }

    @Override
    public float getDistance()
    {
        return (float) Math.pow(10, getActiveCameraSetting().getLog10distance());
    }

    @Override
    public void setDistance(float distance)
    {
        getActiveCameraSetting().setLog10distance(Math.log10(distance));
    }

    @Override
    public Vector3 getTarget()
    {
        return new Vector3((float) getActiveCameraSetting().getxCenter(),
            (float) getActiveCameraSetting().getyCenter(),
            (float) getActiveCameraSetting().getzCenter());
    }

    @Override
    public void setTarget(Vector3 target)
    {
        getActiveCameraSetting().setxCenter(target.x);
        getActiveCameraSetting().setyCenter(target.y);
        getActiveCameraSetting().setzCenter(target.z);
    }

    @Override
    public float getTwist()
    {
        return (float) getActiveCameraSetting().getTwist();
    }

    @Override
    public void setTwist(float twist)
    {
        getActiveCameraSetting().setTwist(twist);
    }

    @Override
    public float getAzimuth()
    {
        return (float) getActiveCameraSetting().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        getActiveCameraSetting().setAzimuth(azimuth);
    }

    @Override
    public float getInclination()
    {
        return (float) getActiveCameraSetting().getInclination();
    }

    @Override
    public void setInclination(float inclination)
    {
        getActiveCameraSetting().setInclination(inclination);
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
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getHorizontalFOV()
    {
        return (float)(getActiveCameraSetting().getFOV() * Math.PI / 180);
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        getActiveCameraSetting().setFOV(fov * 180 / Math.PI);
    }

    @Override
    public float getFocalLength()
    {
        return (float)getActiveCameraSetting().getFocalLength();
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        getActiveCameraSetting().setFocalLength(focalLength);
    }
}
