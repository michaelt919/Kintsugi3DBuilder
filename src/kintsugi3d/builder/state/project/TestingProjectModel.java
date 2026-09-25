/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.project;

import kintsugi3d.builder.core.texture.ImageReplacer;

import java.io.File;
import java.util.List;

public class TestingProjectModel extends ProjectModelBase<SerializableCameraSettings, SerializableEnvironmentSettings,
    SerializableLightGroupSettings<SerializableLightSettings>, SerializableLightSettings, SerializableObjectPoseSettings>
{
    @Override
    public List<SerializableCameraSettings> getCameraList()
    {
        return List.of();
    }

    @Override
    public List<SerializableEnvironmentSettings> getEnvironmentList()
    {
        return List.of();
    }

    @Override
    public List<SerializableLightGroupSettings<SerializableLightSettings>> getLightGroupList()
    {
        return List.of();
    }

    @Override
    public List<SerializableObjectPoseSettings> getObjectPoseList()
    {
        return List.of();
    }

    @Override
    protected SerializableCameraSettings constructCameraSetting()
    {
        return null;
    }

    @Override
    protected SerializableEnvironmentSettings constructEnvironmentSetting()
    {
        return null;
    }

    @Override
    protected SerializableLightGroupSettings<SerializableLightSettings> constructLightGroupSetting()
    {
        return null;
    }

    @Override
    protected SerializableObjectPoseSettings constructObjectPoseSetting()
    {
        return null;
    }

    @Override
    public File getColorCheckerFile()
    {
        return null;
    }

    @Override
    public void setColorCheckerFile(File colorCheckerFile)
    {
    }

    @Override
    public void error(String message, Throwable e)
    {
    }

    @Override
    public void warn(String message, Throwable e)
    {
    }

    @Override
    public void cancelled(String message)
    {
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        new Thread(onConfirm).start();
    }

    @Override
    public void requestUserImageReplacement(ImageReplacer imageReplacer)
    {
    }
}
