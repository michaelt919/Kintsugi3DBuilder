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

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import kintsugi3d.builder.javafx.controllers.scene.environment.ObservableEnvironmentSettings;
import kintsugi3d.builder.state.EnvironmentModelFromSettings;
import kintsugi3d.builder.state.project.EnvironmentSettings;
import kintsugi3d.util.EncodableColorImage;

import java.io.File;
import java.util.Objects;

public class ObservableEnvironmentModel extends EnvironmentModelFromSettings
{
    private ObservableValue<ObservableEnvironmentSettings> selected;
    private final ObservableEnvironmentSettings sentinel = new ObservableEnvironmentSettings("sentinel");

    private final Property<EncodableColorImage> loadedEnvironmentMapImage = new SimpleObjectProperty<>();

    @Override
    protected EnvironmentSettings getEnvironmentSettings()
    {
        if (selected == null || selected.getValue() == null)
        {
            return sentinel;
        }
        else
        {
            return selected.getValue();
        }
    }

    public void setSelected(ObservableValue<ObservableEnvironmentSettings> selected)
    {
        this.selected = selected;
        this.selected.addListener(settingChange);
    }

    @Override
    public boolean isEnabled()
    {
        return selected != null && selected.getValue() != null;
    }

    @Override
    protected void onEnvironmentMapImageLoaded(EncodableColorImage environmentMapImage)
    {
        loadedEnvironmentMapImage.setValue(environmentMapImage);
    }

    public ObservableValue<EncodableColorImage> loadedEnvironmentMapImageProperty()
    {
        return loadedEnvironmentMapImage;
    }

    private final ChangeListener<File> envFileChange = (observable, oldFile, newFile) ->
    {
        if (newFile == null)
        {
            loadedEnvironmentMapImage.setValue(null);
            lastEnvironmentMapTime = 0;
        }
        else if (!Objects.equals(oldFile, newFile) || newFile.lastModified() != lastEnvironmentMapTime)
        {
            loadEnvironmentMap(newFile);
        }
    };

    private final ChangeListener<File> bpFileChange = (observable, oldFile, newFile) ->
    {
        if (newFile != null && (!Objects.equals(oldFile, newFile) || newFile.lastModified() != lastBackplateTime))
        {
            loadBackplate(newFile);
        }
    };

    private final ChangeListener<ObservableEnvironmentSettings> settingChange = (observable, oldSetting, newSetting) ->
    {
        if (newSetting != null)
        {
            newSetting.envImageFileProperty().addListener(envFileChange);
            envFileChange.changed(null, oldSetting == null ? null : oldSetting.getEnvironmentImageFile(), newSetting.getEnvironmentImageFile());

            newSetting.bpImageFileProperty().addListener(bpFileChange);
            bpFileChange.changed(null, oldSetting == null ? null : oldSetting.getBackplateImageFile(), newSetting.getBackplateImageFile());
        }

        if (oldSetting != null)
        {
            oldSetting.envImageFileProperty().removeListener(envFileChange);
            oldSetting.bpImageFileProperty().removeListener(bpFileChange);
        }
    };

}
