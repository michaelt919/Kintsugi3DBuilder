package tetzlaff.ibr.javafx.models;//Created by alexk on 7/25/2017.

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

    private LightInstanceSetting cam()
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
            Matrix4.translate(getCenter().negated())
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
        Vector3 poler = new Vector3((float) cam().getAzimuth(), (float) cam().getInclination(), 0);
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(poler);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 poler = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
        cam().setAzimuth(poler.x);
        cam().setInclination(poler.y);
        orbitCache = orbit;
        fromRender = true;
    }

    @Override
    public float getLog10Distance()
    {
        return (float) cam().getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10distance)
    {
        cam().setLog10distance(log10distance);
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
    public Vector3 getCenter()
    {
        return new Vector3((float) cam().getxCenter(),
            (float) cam().getyCenter(),
            (float) cam().getzCenter());
    }

    @Override
    public void setCenter(Vector3 center)
    {
        cam().setxCenter(center.x);
        cam().setyCenter(center.y);
        cam().setzCenter(center.z);
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
        return (float) cam().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        cam().setAzimuth(azimuth);
    }

    @Override
    public float getInclination()
    {
        return (float) cam().getInclination();
    }

    @Override
    public void setInclination(float inclination)
    {
        cam().setInclination(inclination);
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
        return cam().isLocked() || cam().getGroupLocked();
    }

    @Override
    public Vector3 getColor()
    {
        Color color = cam().getColor();
        Vector3 out = new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
//        System.out.println("Light Color: " + out);
        return out.times((float) cam().getIntensity());
    }

    @Override
    public void setColor(Vector3 color)
    {
        cam().setColor(
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
