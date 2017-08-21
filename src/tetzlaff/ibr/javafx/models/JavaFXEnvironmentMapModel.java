package tetzlaff.ibr.javafx.models;//Created by alexk on 7/28/2017.

import java.io.File;
import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.EnvironmentSettings;
import tetzlaff.models.EnvironmentMapModel;

public class JavaFXEnvironmentMapModel implements EnvironmentMapModel
{
    private ObservableValue<EnvironmentSettings> selected;

    public void setSelected(ObservableValue<EnvironmentSettings> selected)
    {
        this.selected = selected;
        this.selected.addListener(settingChange);
    }

    private boolean doesSelectedExist()
    {
        return selected != null && selected.getValue() != null;
    }

    @Override
    public Vector3 getAmbientLightColor()
    {
        if (doesSelectedExist())
        {
            if (selected.getValue().isEnvUseColorEnabled())
            {
                Color color = selected.getValue().getEnvColor();
                return new Vector3((float) color.getRed(), (float) color.getBlue(), (float) color.getGreen()).times((float) selected.getValue().getEnvColorIntensity());
            }
            else
            {
                if (selected.getValue().isEnvUseImageEnabled())
                {
                    return new Vector3(1.0f);
                }
                else
                {
                    return Vector3.ZERO;
                }
            }
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    private final ChangeListener<File> envFileChange = (observable, oldFile, newFile) ->
    {
        // TODO NEWUI don't reload if oldFile == newFile
        if (newFile != null)
        {
            try
            {
                System.out.println("Loading environment map file " + newFile.getName());
                JavaFXModels.getInstance().getLoadingModel().loadEnvironmentMap(newFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            if (doesSelectedExist())
            {
                selected.getValue().setFirstEnvLoaded(true);
            }
        }
    };

    private final ChangeListener<EnvironmentSettings> settingChange = (observable, oldSetting, newSetting) ->
    {
        if (newSetting != null)
        {
            newSetting.envImageFileProperty().addListener(envFileChange);
            envFileChange.changed(null, null, newSetting.getEnvImageFile());
        }

        if (oldSetting != null)
        {
            oldSetting.envImageFileProperty().removeListener(envFileChange);
        }
    };

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return doesSelectedExist() && selected.getValue().isEnvUseImageEnabled();
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
    public void setAmbientLightColor(Vector3 ambientLightColor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnvironmentMappingEnabled(boolean enabled)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix)
    {
        throw new UnsupportedOperationException();
    }
}
