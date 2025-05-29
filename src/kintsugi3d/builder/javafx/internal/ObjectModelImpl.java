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

package kintsugi3d.builder.javafx.internal;//Created by alexk on 7/21/2017.

import javafx.beans.value.ObservableValue;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.javafx.controllers.scene.object.ObjectPoseSetting;
import kintsugi3d.builder.state.impl.ExtendedObjectModelBase;

public class ObjectModelImpl extends ExtendedObjectModelBase
{
    private ObservableValue<ObjectPoseSetting> selectedObjectPoseProperty;
    private final ObjectPoseSetting sentinel = new ObjectPoseSetting(
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        false,
        1.0,
        "sentinel"
    );

    public ObservableValue<ObjectPoseSetting> getSelectedObjectPoseProperty()
    {
        return this.selectedObjectPoseProperty;
    }

    public void setSelectedObjectPoseProperty(ObservableValue<ObjectPoseSetting> selectedObjectPoseProperty)
    {
        this.selectedObjectPoseProperty = selectedObjectPoseProperty;
    }

    private ObjectPoseSetting getActiveObjectPose()
    {
        if (selectedObjectPoseProperty == null || selectedObjectPoseProperty.getValue() == null)
        {
            return sentinel;
        }
        else
        {
            return selectedObjectPoseProperty.getValue();
        }
    }

    @Override
    public Matrix4 getTransformationMatrix()
    {//TODO: REMOVE DUPLICATE METHOD? (also found in parent class ExtendedObjectModelBase.java)
        return getOrbit().times(Matrix4.translate(getCenter().negated()))
                .times(Matrix4.scale(getScale()));
    }

    @Override
    public Vector3 getCenter()
    {
        return new Vector3((float) getActiveObjectPose().getCenterX(),
            (float) getActiveObjectPose().getCenterY(),
            (float) getActiveObjectPose().getCenterZ());
    }

    @Override
    public void setCenter(Vector3 center)
    {
        if (!getActiveObjectPose().isLocked())
        {
            getActiveObjectPose().setCenterX(center.x);
            getActiveObjectPose().setCenterY(center.y);
            getActiveObjectPose().setCenterZ(center.z);
        }
    }

    @Override
    public float getRotationZ()
    {
        return (float) getActiveObjectPose().getRotateZ();
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        if (!getActiveObjectPose().isLocked())
        {
            getActiveObjectPose().setRotateZ(rotationZ);
        }
    }

    @Override
    public float getRotationY()
    {
        return (float) getActiveObjectPose().getRotateY();
    }

    @Override
    public void setRotationY(float rotationY)
    {
        if (!getActiveObjectPose().isLocked())
        {
            getActiveObjectPose().setRotateY(rotationY);
        }
    }

    @Override
    public float getRotationX()
    {
        return (float) getActiveObjectPose().getRotateX();
    }

    @Override
    public void setRotationX(float rotationX)
    {
        if (!getActiveObjectPose().isLocked())
        {
            getActiveObjectPose().setRotateX(rotationX);
        }
    }

    @Override
    public float getScale(){return (float) getActiveObjectPose().getScale();}

    @Override
    public void setScale(float scale){
        if (!getActiveObjectPose().isLocked())
        {
            getActiveObjectPose().setScale(scale);
        }
    }

    /**
     * this method is intended to return whether or not the selected pose is locked.
     * It is called by the render side of the program, and when it returns true
     * the pose should not be able to be changed using the tools in the render window.
     *
     * @return true for locked
     */
    @Override
    public boolean isLocked()
    {
        return getActiveObjectPose().isLocked();
    }
}
