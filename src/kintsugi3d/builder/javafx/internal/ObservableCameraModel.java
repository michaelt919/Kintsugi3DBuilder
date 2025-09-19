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

package kintsugi3d.builder.javafx.internal;

import javafx.beans.value.ObservableValue;
import kintsugi3d.builder.javafx.controllers.scene.camera.ObservableCameraSettings;
import kintsugi3d.builder.state.CameraModelFromSettings;
import kintsugi3d.builder.state.project.SerializableCameraSettings;

public class ObservableCameraModel extends CameraModelFromSettings
{
    private ObservableValue<ObservableCameraSettings> selectedCameraSetting;
    private final SerializableCameraSettings sentinel = new ObservableCameraSettings("sentinel");

    public ObservableCameraModel()
    {
        sentinel.setLocked(true);
    }

    public void setSelectedCameraSetting(ObservableValue<ObservableCameraSettings> selectedCameraSetting)
    {
        this.selectedCameraSetting = selectedCameraSetting;
    }

    @Override
    protected SerializableCameraSettings getCameraSettings()
    {
        if (selectedCameraSetting == null || selectedCameraSetting.getValue() == null)
        {
            return sentinel;
        }
        else
        {
            return selectedCameraSetting.getValue();
        }
    }
}
