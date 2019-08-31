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

package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.ExtendedObjectModel;
import tetzlaff.models.impl.ExtendedObjectModelBase;

public class ObjectModelWrapper extends ExtendedObjectModelBase
{
    private final ExtendedObjectModel baseModel;
    private final MultithreadValue<Vector3> center;
    private final MultithreadValue<Float> rotationX;
    private final MultithreadValue<Float> rotationY;
    private final MultithreadValue<Float> rotationZ;

    public ObjectModelWrapper(ExtendedObjectModel baseModel)
    {
        this.baseModel = baseModel;
        this.center = MultithreadValue.createFromFunctions(baseModel::getCenter, baseModel::setCenter);
        this.rotationX = MultithreadValue.createFromFunctions(baseModel::getRotationX, baseModel::setRotationX);
        this.rotationY = MultithreadValue.createFromFunctions(baseModel::getRotationY, baseModel::setRotationY);
        this.rotationZ = MultithreadValue.createFromFunctions(baseModel::getRotationZ, baseModel::setRotationZ);
    }

    @Override
    public boolean isLocked()
    {
        return baseModel.isLocked();
    }

    @Override
    public Vector3 getCenter()
    {
        return this.center.getValue();
    }

    @Override
    public float getRotationZ()
    {
        return this.rotationZ.getValue();
    }

    @Override
    public float getRotationY()
    {
        return this.rotationY.getValue();
    }

    @Override
    public float getRotationX()
    {
        return this.rotationX.getValue();
    }

    @Override
    public void setCenter(Vector3 center)
    {
        this.center.setValue(center);
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        this.rotationZ.setValue(rotationZ);
    }

    @Override
    public void setRotationY(float rotationY)
    {
        this.rotationY.setValue(rotationY);
    }

    @Override
    public void setRotationX(float rotationX)
    {
        this.rotationX.setValue(rotationX);
    }
}
