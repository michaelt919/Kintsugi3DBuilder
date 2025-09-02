/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.internal;//Created by alexk on 7/25/2017.

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import kintsugi3d.builder.javafx.controllers.scene.lights.ObservableLightInstanceSetting;
import kintsugi3d.builder.state.LightInstanceModel;
import kintsugi3d.builder.state.impl.ExtendedViewpointModelBase;
import kintsugi3d.builder.state.project.LightInstanceSetting;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public class ObservableLightInstanceModel extends ExtendedViewpointModelBase implements LightInstanceModel
{
    private ObservableValue<ObservableLightInstanceSetting> subLightSettingObservableValue;
    private final LightInstanceSetting sentinel;

    public ObservableLightInstanceModel()
    {
        this.sentinel = new ObservableLightInstanceSetting("sentinel", new SimpleBooleanProperty(true));
        this.sentinel.setIntensity(0.0);
        this.sentinel.setLocked(true);
    }

    public void setSubLightSettingObservableValue(ObservableValue<ObservableLightInstanceSetting> subLightSettingObservableValue)
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
    public float getLog10Distance()
    {
        return (float) getLightInstance().getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        if (!this.isLocked())
        {
            getLightInstance().setLog10Distance(log10Distance);
        }
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
        if (!this.isLocked())
        {
            getLightInstance().setTargetX(target.x);
            getLightInstance().setTargetY(target.y);
            getLightInstance().setTargetZ(target.z);
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
        return (float) getLightInstance().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        if (!this.isLocked())
        {
            getLightInstance().setAzimuth(azimuth);
        }
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
        if (!this.isLocked())
        {
            getLightInstance().setInclination(inclination);
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
        if (!this.isLocked())
        {
            LightInstanceSetting lightInstance = getLightInstance();
            double intensity = lightInstance.getIntensity();

            if (intensity > 0.0)
            {
                lightInstance.setColor(new Color(color.x / intensity, color.y / intensity, color.z / intensity, 1));
            }
            else
            {
                lightInstance.setIntensity(1.0);
                lightInstance.setColor(new Color(color.x, color.y, color.z, 1));
            }
        }
    }

    @Override
    public float getSpotSize()
    {
        return (float)(getLightInstance().getSpotSize() * Math.PI / 180.0);
    }

    @Override
    public void setSpotSize(float spotSize)
    {
        if (!this.isLocked())
        {
            getLightInstance().setSpotSize(spotSize * 180 / Math.PI);
        }
    }

    @Override
    public float getSpotTaper()
    {
        return (float)getLightInstance().getSpotTaper();
    }

    @Override
    public void setSpotTaper(float spotTaper)
    {
        if (!this.isLocked())
        {
            getLightInstance().setSpotTaper(spotTaper);
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
