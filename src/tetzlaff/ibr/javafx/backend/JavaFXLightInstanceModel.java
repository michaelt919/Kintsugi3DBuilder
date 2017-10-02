package tetzlaff.ibr.javafx.backend;//Created by alexk on 7/25/2017.

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightInstanceSetting;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightType;
import tetzlaff.models.LightInstanceModel;
import tetzlaff.util.OrbitPolarConverter;

public class JavaFXLightInstanceModel implements LightInstanceModel
{
    private ObservableValue<LightInstanceSetting> subLightSettingObservableValue;
    private final LightInstanceSetting backup = new LightInstanceSetting(
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        true,
        "backup",
        LightType.PointLight,
        Color.BLACK,
        new SimpleBooleanProperty(true));

    public void setSubLightSettingObservableValue(ObservableValue<LightInstanceSetting> subLightSettingObservableValue)
    {
        this.subLightSettingObservableValue = subLightSettingObservableValue;
    }

    private LightInstanceSetting getLightInstance()
    {
        if (subLightSettingObservableValue == null || subLightSettingObservableValue.getValue() == null)
        {
//            System.out.println("Using SubLight Backup");
            return backup;
        }
        else
        {
//            System.out.println("Win");
            return subLightSettingObservableValue.getValue();
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
        Vector3 polar = new Vector3((float) getLightInstance().getAzimuth(), (float) getLightInstance().getInclination(), 0);
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
        getLightInstance().setAzimuth(polar.x);
        getLightInstance().setInclination(polar.y);
        orbitCache = orbit;
        fromRender = true;
    }

    @Override
    public float getLog10Distance()
    {
        return (float) getLightInstance().getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10distance)
    {
        getLightInstance().setLog10Distance(log10distance);
    }

    @Override
    public float getDistance()
    {
        return (float) Math.pow(10, getLog10Distance());
    }

    @Override
    public void setDistance(float distance)
    {
        setLog10Distance((float) Math.log10(distance));
    }

    @Override
    public Vector3 getTarget()
    {
        return new Vector3((float) getLightInstance().getTargetX(),
            (float) getLightInstance().getTargetY(),
            (float) getLightInstance().getTargetZ());
    }

    @Override
    public void setTarget(Vector3 target)
    {
        getLightInstance().setTargetX(target.x);
        getLightInstance().setTargetY(target.y);
        getLightInstance().setTargetZ(target.z);
    }

    @Override
    public float getTwist()
    {
        return 0.0f;
    }

    @Override
    public void setTwist(float twist)
    {
    }

    @Override
    public float getAzimuth()
    {
        return (float) getLightInstance().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        getLightInstance().setAzimuth(azimuth);
    }

    @Override
    public float getInclination()
    {
        return (float) getLightInstance().getInclination();
    }

    @Override
    public float getFocalLength()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInclination(float inclination)
    {
        getLightInstance().setInclination(inclination);
    }

    @Override
    public float getHorizontalFOV()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOrthographic()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        throw new UnsupportedOperationException();
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
        return getLightInstance().isLocked() || getLightInstance().isGroupLocked();
    }

    @Override
    public Vector3 getColor()
    {
        Color color = getLightInstance().getColor();
        Vector3 out = new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
//        System.out.println("Light Color: " + out);
        return out.times((float) getLightInstance().getIntensity());
    }

    @Override
    public void setColor(Vector3 color)
    {
        getLightInstance().setColor(
            new Color(color.x, color.y, color.z, 1)
        );
    }

    @Override
    public boolean isEnabled()
    {
        return !(subLightSettingObservableValue == null || subLightSettingObservableValue.getValue() == null);
    }

    @Override
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        throw new UnsupportedOperationException();
    }
}
