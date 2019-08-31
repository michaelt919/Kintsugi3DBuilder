/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.internal;//Created by alexk on 7/25/2017.

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.controllers.scene.lights.LightInstanceSetting;
import tetzlaff.models.LightInstanceModel;
import tetzlaff.util.OrbitPolarConverter;

public class LightInstanceModelImpl implements LightInstanceModel
{
    private ObservableValue<LightInstanceSetting> subLightSettingObservableValue;
    private final LightInstanceSetting sentinel;

    public LightInstanceModelImpl()
    {
        this.sentinel = new LightInstanceSetting("sentinel", new SimpleBooleanProperty(true));
        this.sentinel.intensity().set(0.0);
        this.sentinel.locked().set(true);
    }

    public void setSubLightSettingObservableValue(ObservableValue<LightInstanceSetting> subLightSettingObservableValue)
    {
        this.subLightSettingObservableValue = subLightSettingObservableValue;
    }

    private LightInstanceSetting getLightInstance()
    {
        if (subLightSettingObservableValue == null || subLightSettingObservableValue.getValue() == null)
        {
            return sentinel;
        }
        else
        {
            return subLightSettingObservableValue.getValue();
        }
    }

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
        Vector3 polar = new Vector3((float) getLightInstance().azimuth().get(), (float) getLightInstance().inclination().get(), 0);
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        if (!this.isLocked())
        {
            Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
            getLightInstance().azimuth().set((double) polar.x);
            getLightInstance().inclination().set((double) polar.y);
        }
    }

    @Override
    public float getLog10Distance()
    {
        return (float) getLightInstance().log10Distance().get();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        if (!this.isLocked())
        {
            getLightInstance().log10Distance().set((double) log10Distance);
        }
    }

    @Override
    public float getDistance()
    {
        return (float) Math.pow(10, getLog10Distance());
    }

    @Override
    public void setDistance(float distance)
    {
        this.setLog10Distance((float) Math.log10(distance));
    }

    @Override
    public Vector3 getTarget()
    {
        return new Vector3((float) getLightInstance().targetX().get(),
            (float) getLightInstance().targetY().get(),
            (float) getLightInstance().targetZ().get());
    }

    @Override
    public void setTarget(Vector3 target)
    {
        if (!this.isLocked())
        {
            getLightInstance().targetX().set((double) target.x);
            getLightInstance().targetY().set((double) target.y);
            getLightInstance().targetZ().set((double) target.z);
        }
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
        return (float) getLightInstance().azimuth().get();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        if (!this.isLocked())
        {
            getLightInstance().azimuth().set((double) azimuth);
        }
    }

    @Override
    public float getInclination()
    {
        return (float) getLightInstance().inclination().get();
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
        if (!this.isLocked())
        {
            getLightInstance().inclination().set((double) inclination);
        }
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
        return getLightInstance().locked().get() || getLightInstance().isGroupLocked();
    }

    @Override
    public Vector3 getColor()
    {
        Color color = getLightInstance().color().getValue();
        Vector3 out = new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
//        System.out.println("Light Color: " + out);
        return out.times((float) getLightInstance().intensity().get());
    }

    @Override
    public void setColor(Vector3 color)
    {
        if (!this.isLocked())
        {
            LightInstanceSetting lightInstance = getLightInstance();
            double intensity = lightInstance.intensity().get();

            if (intensity > 0.0)
            {
                lightInstance.color().setValue(new Color(color.x / intensity, color.y / intensity, color.z / intensity, 1));
            }
            else
            {
                lightInstance.intensity().set(1.0);
                lightInstance.color().setValue(new Color(color.x, color.y, color.z, 1));
            }
        }
    }

    @Override
    public float getSpotSize()
    {
        return (float)(getLightInstance().spotSize().get() * Math.PI / 180.0);
    }

    @Override
    public void setSpotSize(float spotSize)
    {
        if (!this.isLocked())
        {
            getLightInstance().spotSize().set(spotSize * 180 / Math.PI);
        }
    }

    @Override
    public float getSpotTaper()
    {
        return (float)getLightInstance().spotTaper().get();
    }

    @Override
    public void setSpotTaper(float spotTaper)
    {
        if (!this.isLocked())
        {
            getLightInstance().spotTaper().set(spotTaper);
        }
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
