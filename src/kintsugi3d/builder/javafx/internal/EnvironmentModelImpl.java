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

package kintsugi3d.builder.javafx.internal;//Created by alexk on 7/28/2017.

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.scene.environment.EnvironmentSetting;
import kintsugi3d.builder.state.BackgroundMode;
import kintsugi3d.builder.state.EnvironmentModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.EncodableColorImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentModelImpl implements EnvironmentModel
{
    private static final Logger log = LoggerFactory.getLogger(EnvironmentModelImpl.class);
    private ObservableValue<EnvironmentSetting> selected;

    private boolean environmentMapLoaded = false;
    private boolean backplateLoaded = false;

    private long lastEnvironmentMapTime;
    private long lastBackplateTime;

    private final Property<EncodableColorImage> loadedEnvironmentMapImage = new SimpleObjectProperty<>();

    public void setSelected(ObservableValue<EnvironmentSetting> selected)
    {
        this.selected = selected;
        this.selected.addListener(settingChange);
    }

    private boolean doesSelectedExist()
    {
        return selected != null && selected.getValue() != null;
    }

    public ObservableValue<EncodableColorImage> loadedEnvironmentMapImageProperty()
    {
        return loadedEnvironmentMapImage;
    }

    @Override
    public Vector3 getEnvironmentColor()
    {
        if (doesSelectedExist())
        {
            EnvironmentSetting selectedEnvironment = selected.getValue();
            if (selectedEnvironment.isEnvUseColorEnabled() && (environmentMapLoaded || !selectedEnvironment.isEnvUseImageEnabled()))
            {
                Color color = selectedEnvironment.getEnvColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float) selectedEnvironment.getEnvIntensity());
            }
            else if (selectedEnvironment.isEnvUseImageEnabled() && environmentMapLoaded)
            {
                return new Vector3((float) selectedEnvironment.getEnvIntensity());
            }
            else
            {
                return Vector3.ZERO;
            }
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    @Override
    public Vector3 getBackgroundColor()
    {
        if (doesSelectedExist())
        {
            EnvironmentSetting selectedEnvironment = selected.getValue();

            if (selectedEnvironment.isBPUseColorEnabled() && (backplateLoaded || !selectedEnvironment.isBPUseImageEnabled()))
            {
                Color color = selectedEnvironment.getBpColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float) selectedEnvironment.getBackgroundIntensity());
            }
            else if (selectedEnvironment.isBPUseImageEnabled() && backplateLoaded)
            {
                return new Vector3((float) selectedEnvironment.getBackgroundIntensity());
            }
            else if (selectedEnvironment.isEnvUseColorEnabled() && (environmentMapLoaded || !selectedEnvironment.isEnvUseImageEnabled()))
            {
                Color color = selectedEnvironment.getEnvColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float)(selectedEnvironment.getBackgroundIntensity() * selectedEnvironment.getEnvIntensity()));
            }
            else if (selectedEnvironment.isEnvUseImageEnabled() && environmentMapLoaded)
            {
                return new Vector3((float)(selectedEnvironment.getBackgroundIntensity() * selectedEnvironment.getEnvIntensity()));
            }
            else
            {
                return Vector3.ZERO;
            }
        }
        else
        {
            return Vector3.ZERO;
        }
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

    public void loadEnvironmentMap(File newFile)
    {
        environmentMapLoaded = false;
        long lastModified = newFile.lastModified();

        new Thread(() ->
        {
            try
            {
                log.info("Loading environment map file " + newFile.getName());
                Optional<EncodableColorImage> environmentMapImage = MultithreadModels.getInstance().getLoadingModel().loadEnvironmentMap(newFile);
                loadedEnvironmentMapImage.setValue(environmentMapImage.orElse(null));
            }
            catch (FileNotFoundException e)
            {
                log.error("Failed to find environment map file '{}':", newFile.getName(), e);
            }

            if (doesSelectedExist())
            {
                selected.getValue().setEnvLoaded(true);
            }

            environmentMapLoaded = true;
            lastEnvironmentMapTime = lastModified;

        }).start();
    }

    private final ChangeListener<File> bpFileChange = (observable, oldFile, newFile) ->
    {
        if (newFile != null && (!Objects.equals(oldFile, newFile) || newFile.lastModified() != lastBackplateTime))
        {
            loadBackplate(newFile);
        }
    };

    public void loadBackplate(File newFile)
    {
        backplateLoaded = false;
        lastBackplateTime = newFile.lastModified();
        backplateLoaded = true;

        new Thread(() ->
        {
            try
            {
                log.info("Loading backplate file " + newFile.getName());
                MultithreadModels.getInstance().getLoadingModel().loadBackplate(newFile);
            }
            catch (FileNotFoundException e)
            {
                log.error("Failed to find backplate file '{}':", newFile.getName(), e);
            }

            if (doesSelectedExist())
            {
                selected.getValue().setBPLoaded(true);
            }
        })
        .start();
    }

    private final ChangeListener<EnvironmentSetting> settingChange = (observable, oldSetting, newSetting) ->
    {
        if (newSetting != null)
        {
            newSetting.envImageFileProperty().addListener(envFileChange);
            envFileChange.changed(null, oldSetting == null ? null : oldSetting.getEnvImageFile(), newSetting.getEnvImageFile());

            newSetting.bpImageFileProperty().addListener(bpFileChange);
            bpFileChange.changed(null, oldSetting == null ? null : oldSetting.getBpImageFile(), newSetting.getBpImageFile());
        }

        if (oldSetting != null)
        {
            oldSetting.envImageFileProperty().removeListener(envFileChange);
            oldSetting.bpImageFileProperty().removeListener(bpFileChange);
        }
    };

    @Override
    public BackgroundMode getBackgroundMode()
    {
        EnvironmentSetting selectedEnvironment = selected.getValue();
        if (this.backplateLoaded && doesSelectedExist() && selectedEnvironment.isBPUseImageEnabled())
        {
            return BackgroundMode.IMAGE;
        }
        else if (selectedEnvironment.isBPUseColorEnabled())
        {
            return BackgroundMode.COLOR;
        }
        else if (isEnvironmentMappingEnabled())
        {
            return BackgroundMode.ENVIRONMENT_MAP;
        }
        else
        {
            return BackgroundMode.NONE;
        }
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return this.environmentMapLoaded && doesSelectedExist() && selected.getValue().isEnvUseImageEnabled();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix()
    {
        if (doesSelectedExist())
        {
            double azimuth = selected.getValue().getEnvRotation();
            return Matrix4.rotateY(Math.toRadians(azimuth));
        }
        else
        {
            return Matrix4.IDENTITY;
        }
    }

    @Override
    public float getEnvironmentMapFilteringBias()
    {
        return doesSelectedExist() ? (float)selected.getValue().getEnvFilteringBias() : 0;
    }

    @Override
    public float getEnvironmentRotation()
    {
        return doesSelectedExist() ? (float)(selected.getValue().getEnvRotation() * Math.PI / 180) : 0.0f;
    }

    @Override
    public void setEnvironmentRotation(float environmentRotation)
    {
        if (doesSelectedExist() && !selected.getValue().isLocked())
        {
            selected.getValue().setEnvRotation(environmentRotation * 180 / Math.PI);
        }
    }

    @Override
    public float getEnvironmentIntensity()
    {
        return doesSelectedExist() ? (float)selected.getValue().getEnvIntensity() : 0.0f;
    }

    @Override
    public void setEnvironmentIntensity(float environmentIntensity)
    {
        if (doesSelectedExist() && !selected.getValue().isLocked())
        {
            selected.getValue().setEnvIntensity(environmentIntensity);
        }
    }

    @Override
    public float getBackgroundIntensity()
    {
        return doesSelectedExist() ? (float)selected.getValue().getBackgroundIntensity() : 0.0f;
    }

    @Override
    public void setBackgroundIntensity(float backgroundIntensity)
    {
        if (doesSelectedExist() && !selected.getValue().isLocked())
        {
            selected.getValue().setBackgroundIntensity(backgroundIntensity);
        }
    }

    @Override
    public boolean isGroundPlaneEnabled()
    {
        return doesSelectedExist() && selected.getValue().isGPEnabled();
    }

    @Override
    public Vector3 getGroundPlaneColor()
    {
        if (doesSelectedExist())
        {
            Color color = selected.getValue().getGPColor();
            return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
        }
        else
        {
            return new Vector3(1);
        }

    }

    @Override
    public float getGroundPlaneHeight()
    {
        return doesSelectedExist() ? (float)selected.getValue().getGPHeight() : 0.0f;
    }

    @Override
    public float getGroundPlaneSize()
    {
        return doesSelectedExist() ? (float)selected.getValue().getGPSize() : 1.0f;
    }
}
