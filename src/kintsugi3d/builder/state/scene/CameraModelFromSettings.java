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
