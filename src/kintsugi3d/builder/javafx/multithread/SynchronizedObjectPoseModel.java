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

package kintsugi3d.builder.javafx.multithread;

import kintsugi3d.builder.state.ManipulableObjectPoseModel;
import kintsugi3d.builder.state.ObjectPoseModelBase;
import kintsugi3d.gl.vecmath.Vector3;

public class SynchronizedObjectPoseModel extends ObjectPoseModelBase
{
    private final ManipulableObjectPoseModel baseModel;
    private final SynchronizedValue<Vector3> center;
    private final SynchronizedValue<Float> rotationX;
    private final SynchronizedValue<Float> rotationY;
    private final SynchronizedValue<Float> rotationZ;

    private final SynchronizedValue<Float> scale;

    public SynchronizedObjectPoseModel(ManipulableObjectPoseModel baseModel)
    {
        this.baseModel = baseModel;
        this.center = SynchronizedValue.createFromFunctions(baseModel::getCenter, baseModel::setCenter);
        this.rotationX = SynchronizedValue.createFromFunctions(baseModel::getRotationX, baseModel::setRotationX);
        this.rotationY = SynchronizedValue.createFromFunctions(baseModel::getRotationY, baseModel::setRotationY);
        this.rotationZ = SynchronizedValue.createFromFunctions(baseModel::getRotationZ, baseModel::setRotationZ);
        this.scale = SynchronizedValue.createFromFunctions(baseModel::getScale, baseModel::setScale);
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
    public float getScale(){return this.scale.getValue();}

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

    @Override
    public void setScale(float scale){this.scale.setValue(scale);}
}
