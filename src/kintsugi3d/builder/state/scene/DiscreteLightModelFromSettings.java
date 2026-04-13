/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.scene;

import javafx.scene.paint.Color;
import kintsugi3d.builder.state.project.LightSettings;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public abstract class DiscreteLightModelFromSettings extends ViewpointModelBase implements DiscreteLightModel
{
    protected abstract LightSettings getLightSettings();

    @Override
    public float getLog10Distance()
    {
        return (float) getLightSettings().getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        if (!this.isLocked())
        {
            getLightSettings().setLog10Distance(log10Distance);
        }
    }

    @Override
    public Vector3 getTarget()
    {
        return new Vector3((float) getLightSettings().getTargetX(),
            (float) getLightSettings().getTargetY(),
            (float) getLightSettings().getTargetZ());
    }

    @Override
    public void setTarget(Vector3 target)
    {
        if (!this.isLocked())
        {
            getLightSettings().setTargetX(target.x);
            getLightSettings().setTargetY(target.y);
            getLightSettings().setTargetZ(target.z);
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
        return (float) getLightSettings().getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        if (!this.isLocked())
        {
            getLightSettings().setAzimuth(azimuth);
        }
    }

    @Override
    public float getInclination()
    {
        return (float) getLightSettings().getInclination();
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
            getLightSettings().setInclination(inclination);
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
        return getLightSettings().isLocked() || getLightSettings().isGroupLocked();
    }

    @Override
    public Vector3 getColor()
    {
        Color color = getLightSettings().getColor();
        Vector3 out = new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
//        System.out.println("Light Color: " + out);
        return out.times((float) getLightSettings().getIntensity());
    }

    @Override
    public void setColor(Vector3 color)
    {
        if (!this.isLocked())
        {
            LightSettings lightInstance = getLightSettings();
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
        return (float) (getLightSettings().getSpotSize() * Math.PI / 180.0);
    }

    @Override
    public void setSpotSize(float spotSize)
    {
        if (!this.isLocked())
        {
            getLightSettings().setSpotSize(spotSize * 180 / Math.PI);
        }
    }

    @Override
    public float getSpotTaper()
    {
        return (float) getLightSettings().getSpotTaper();
    }

    @Override
    public void setSpotTaper(float spotTaper)
    {
        if (!this.isLocked())
        {
            getLightSettings().setSpotTaper(spotTaper);
        }
    }

    @Override
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        throw new UnsupportedOperationException();
    }
}
