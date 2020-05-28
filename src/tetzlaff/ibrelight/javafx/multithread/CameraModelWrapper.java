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

package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.impl.ExtendedCameraModelBase;

public class CameraModelWrapper extends ExtendedCameraModelBase
{
    private final ExtendedCameraModel baseModel;

    private final MultithreadValue<Vector3> target;
    private final MultithreadValue<Float> azimuth;
    private final MultithreadValue<Float> inclination;
    private final MultithreadValue<Float> log10Distance;
    private final MultithreadValue<Float> twist;
    private final MultithreadValue<Float> horizontalFOV;
    private final MultithreadValue<Float> focalLength;
    private final MultithreadValue<Boolean> orthographic;

    public CameraModelWrapper(ExtendedCameraModel baseModel)
    {
        this.baseModel = baseModel;
        this.target = MultithreadValue.createFromFunctions(baseModel::getTarget, baseModel::setTarget);
        this.azimuth = MultithreadValue.createFromFunctions(baseModel::getAzimuth, baseModel::setAzimuth);
        this.inclination = MultithreadValue.createFromFunctions(baseModel::getInclination, baseModel::setInclination);
        this.log10Distance = MultithreadValue.createFromFunctions(baseModel::getLog10Distance, baseModel::setLog10Distance);
        this.twist = MultithreadValue.createFromFunctions(baseModel::getTwist, baseModel::setTwist);
        this.horizontalFOV = MultithreadValue.createFromFunctions(baseModel::getHorizontalFOV, baseModel::setHorizontalFOV);
        this.focalLength = MultithreadValue.createFromFunctions(baseModel::getFocalLength, baseModel::setFocalLength);
        this.orthographic = MultithreadValue.createFromFunctions(baseModel::isOrthographic, baseModel::setOrthographic);
    }

    @Override
    public boolean isLocked()
    {
        return baseModel.isLocked();
    }

    @Override
    public Vector3 getTarget()
    {
        return this.target.getValue();
    }

    @Override
    public void setTarget(Vector3 target)
    {
        this.target.setValue(target);
    }

    @Override
    public boolean isOrthographic()
    {
        return this.orthographic.getValue();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        this.orthographic.setValue(orthographic);
    }

    @Override
    public float getHorizontalFOV()
    {
        return this.horizontalFOV.getValue();
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        this.horizontalFOV.setValue(fov);
    }

    @Override
    public float getLog10Distance()
    {
        return this.log10Distance.getValue();
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        this.log10Distance.setValue(log10Distance);
    }

    @Override
    public float getTwist()
    {
        return this.twist.getValue();
    }

    @Override
    public void setTwist(float twist)
    {
        this.twist.setValue(twist);
    }

    @Override
    public float getAzimuth()
    {
        return this.azimuth.getValue();
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        this.azimuth.setValue(azimuth);
    }

    @Override
    public float getInclination()
    {
        return this.inclination.getValue();
    }

    @Override
    public void setInclination(float inclination)
    {
        this.inclination.setValue(inclination);
    }

    @Override
    public float getFocalLength()
    {
        return this.focalLength.getValue();
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        this.focalLength.setValue(focalLength);
    }
}
