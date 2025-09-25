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
import kintsugi3d.builder.javafx.controllers.scene.object.ObservableObjectPoseSettings;
import kintsugi3d.builder.state.project.ObjectPoseSettings;
import kintsugi3d.builder.state.project.SerializableObjectPoseSettings;
import kintsugi3d.builder.state.scene.ObjectPoseModelFromSettings;

public class ObservableObjectPoseModel extends ObjectPoseModelFromSettings
{
    private ObservableValue<ObservableObjectPoseSettings> selectedObjectPoseProperty;
    private final SerializableObjectPoseSettings sentinel = new ObservableObjectPoseSettings("sentinel");

    public ObservableValue<ObservableObjectPoseSettings> getSelectedObjectPoseProperty()
    {
        return this.selectedObjectPoseProperty;
    }

    public void setSelectedObjectPoseProperty(ObservableValue<ObservableObjectPoseSettings> selectedObjectPoseProperty)
    {
        this.selectedObjectPoseProperty = selectedObjectPoseProperty;
    }

    @Override
    protected ObjectPoseSettings getObjectPoseSettings()
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
}
