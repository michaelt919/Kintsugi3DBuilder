package tetzlaff.ibrelight.javafx.internal;//Created by alexk on 7/28/2017.

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.MultithreadModels;
import tetzlaff.ibrelight.javafx.controllers.scene.environment.EnvironmentSetting;
import tetzlaff.models.EnvironmentModel;

public class EnvironmentModelImpl implements EnvironmentModel
{
    private ObservableValue<EnvironmentSetting> selected;

    private boolean environmentMapLoaded = false;

    public void setSelected(ObservableValue<EnvironmentSetting> selected)
    {
        this.selected = selected;
        this.selected.addListener(settingChange);
    }

    private boolean doesSelectedExist()
    {
        return selected != null && selected.getValue() != null;
    }

    @Override
    public Vector3 getEnvironmentColor()
    {
        if (doesSelectedExist())
        {
            if (selected.getValue().isEnvUseColorEnabled())
            {
                Color color = selected.getValue().getEnvColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float)selected.getValue().getEnvIntensity());
            }
            else  if (selected.getValue().isEnvUseImageEnabled())
            {
                return new Vector3((float)selected.getValue().getEnvIntensity());
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
        if (newFile != null && !Objects.equals(oldFile, newFile))
        {
            environmentMapLoaded = false;

            new Thread(() ->
            {
                try
                {
                    System.out.println("Loading environment map file " + newFile.getName());
                    MultithreadModels.getInstance().getLoadingModel().loadEnvironmentMap(newFile);
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }

                if (doesSelectedExist())
                {
                    selected.getValue().setFirstEnvLoaded(true);
                }

                environmentMapLoaded = true;

            }).start();
        }
    };

    private final ChangeListener<EnvironmentSetting> settingChange = (observable, oldSetting, newSetting) ->
    {
        if (newSetting != null)
        {
            newSetting.envImageFileProperty().addListener(envFileChange);
            envFileChange.changed(null, oldSetting == null ? null : oldSetting.getEnvImageFile(), newSetting.getEnvImageFile());
        }

        if (oldSetting != null)
        {
            oldSetting.envImageFileProperty().removeListener(envFileChange);
        }
    };

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
    public float getEnvironmentRotation()
    {
        return doesSelectedExist() ? (float)(selected.getValue().getEnvRotation() * Math.PI / 180) : 0.0f;
    }

    @Override
    public void setEnvironmentRotation(float environmentRotation)
    {
        if (doesSelectedExist())
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
        if (doesSelectedExist())
        {
            selected.getValue().setEnvIntensity(environmentIntensity);
        }
    }
}
