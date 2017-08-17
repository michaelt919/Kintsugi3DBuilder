package tetzlaff.ibr.javafx.models;//Created by alexk on 7/28/2017.

import java.io.File;
import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.EnvironmentSettings;
import tetzlaff.models.ReadonlyEnvironmentMapModel;

public class JavaFXEnvironmentMapModel implements ReadonlyEnvironmentMapModel
{
    private ObservableValue<EnvironmentSettings> selected;

    public JavaFXEnvironmentMapModel()
    {
    }

    public void setSelected(ObservableValue<EnvironmentSettings> selected)
    {
        this.selected = selected;
        this.selected.addListener(settingChange);
    }

    private boolean selectedExists()
    {
        return selected != null && selected.getValue() != null;
    }

    @Override
    public Vector3 getAmbientLightColor()
    {
        if (selectedExists())
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
                    return new Vector3(1f);
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

            if (selectedExists())
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
    public boolean getEnvironmentMappingEnabled()
    {
        if (selectedExists())
        {
            return selected.getValue().isEnvUseImageEnabled();
        }
        else
        {
            return false;
        }
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix()
    {
        if (selectedExists())
        {
            double azmuth = selected.getValue().getEnvRotation();
            return Matrix4.rotateY(Math.toRadians(azmuth));
        }
        else
        {
            return Matrix4.IDENTITY;
        }
    }
}
