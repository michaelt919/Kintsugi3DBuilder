/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.LightInstanceModel;

public abstract class LightInstanceModelBase implements LightInstanceModel
{
    private final ExtendedCameraModel lightCameraModel;

    protected LightInstanceModelBase(ExtendedCameraModel lightCameraModel)
    {
        this.lightCameraModel = lightCameraModel;
    }

    @Override
    public boolean isLocked()
    {
        return lightCameraModel.isLocked();
    }

    @Override
    public Matrix4 getLookMatrix()
    {
        return lightCameraModel.getLookMatrix();
    }

    @Override
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        lightCameraModel.setLookMatrix(lookMatrix);
    }

    @Override
    public Matrix4 getOrbit()
    {
        return lightCameraModel.getOrbit();
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        lightCameraModel.setOrbit(orbit);
    }

    @Override
    public float getDistance()
    {
        return lightCameraModel.getDistance();
    }

    @Override
    public void setDistance(float distance)
    {
        lightCameraModel.setDistance(distance);
    }

    @Override
    public Vector3 getTarget()
    {
        return lightCameraModel.getTarget();
    }

    @Override
    public void setTarget(Vector3 target)
    {
        lightCameraModel.setTarget(target);
    }

    @Override
    public float getAzimuth()
    {
        return lightCameraModel.getAzimuth();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        lightCameraModel.setAzimuth(azimuth);
    }

    @Override
    public float getInclination()
    {
        return lightCameraModel.getInclination();
    }

    @Override
    public void setInclination(float inclination)
    {
        lightCameraModel.setInclination(inclination);
    }

    @Override
    public float getLog10Distance()
    {
        return lightCameraModel.getLog10Distance();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        lightCameraModel.setLog10Distance(log10Distance);
    }

    @Override
    public float getTwist()
    {
        return lightCameraModel.getTwist();
    }

    @Override
    public void setTwist(float twist)
    {
        lightCameraModel.setTwist(twist);
    }

    @Override
    public float getHorizontalFOV()
    {
        return lightCameraModel.getHorizontalFOV();
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        lightCameraModel.setHorizontalFOV(fov);
    }

    @Override
    public float getFocalLength()
    {
        return lightCameraModel.getFocalLength();
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        lightCameraModel.setFocalLength(focalLength);
    }

    @Override
    public boolean isOrthographic()
    {
        return lightCameraModel.isOrthographic();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        lightCameraModel.setOrthographic(orthographic);
    }
}
