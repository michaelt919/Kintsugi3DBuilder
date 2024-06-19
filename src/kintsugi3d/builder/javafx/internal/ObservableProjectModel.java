/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.internal;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import kintsugi3d.builder.javafx.controllers.scene.camera.CameraSetting;
import kintsugi3d.builder.javafx.controllers.scene.environment.EnvironmentSetting;
import kintsugi3d.builder.javafx.controllers.scene.lights.LightGroupSetting;
import kintsugi3d.builder.javafx.controllers.scene.object.ObjectPoseSetting;

public class ObservableProjectModel extends SynchronizedProjectModel
{
    private final ObservableList<CameraSetting> cameraList = new ObservableListWrapper<>(super.getCameraList());
    private final ObservableList<EnvironmentSetting> environmentList = new ObservableListWrapper<>(super.getEnvironmentList());
    private final ObservableList<LightGroupSetting> lightGroupList = new ObservableListWrapper<>(super.getLightGroupList());
    private final ObservableList<ObjectPoseSetting> objectPoseList = new ObservableListWrapper<>(super.getObjectPoseList());

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<CameraSetting> getCameraList()
    {
        return this.cameraList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<EnvironmentSetting> getEnvironmentList()
    {
        return this.environmentList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<LightGroupSetting> getLightGroupList()
    {
        return this.lightGroupList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObjectPoseSetting> getObjectPoseList()
    {
        return this.objectPoseList;
    }
}
