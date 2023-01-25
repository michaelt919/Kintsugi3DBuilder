/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.internal;

import tetzlaff.ibrelight.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.ibrelight.javafx.controllers.scene.environment.EnvironmentSetting;
import tetzlaff.ibrelight.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibrelight.javafx.controllers.scene.object.ObjectPoseSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynchronizedProjectModel extends ProjectModelBase
{
    private final List<CameraSetting> cameraList = Collections.synchronizedList(new ArrayList<>(16));
    private final List<EnvironmentSetting> environmentList = Collections.synchronizedList(new ArrayList<>(16));
    private final List<LightGroupSetting> lightGroupList = Collections.synchronizedList(new ArrayList<>(16));
    private final List<ObjectPoseSetting> objectPoseList = Collections.synchronizedList(new ArrayList<>(16));

    @Override
    public List<CameraSetting> getCameraList()
    {
        return this.cameraList;
    }

    @Override
    public List<EnvironmentSetting> getEnvironmentList()
    {
        return this.environmentList;
    }

    @Override
    public List<LightGroupSetting> getLightGroupList()
    {
        return this.lightGroupList;
    }

    @Override
    public List<ObjectPoseSetting> getObjectPoseList()
    {
        return this.objectPoseList;
    }

}
