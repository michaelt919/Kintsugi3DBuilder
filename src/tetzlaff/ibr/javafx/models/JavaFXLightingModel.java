package tetzlaff.ibr.javafx.models;//Created by alexk on 7/25/2017.

import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.models.EnvironmentMapModel;
import tetzlaff.models.LightInstanceModel;
import tetzlaff.models.impl.LightingModelBase;

public class JavaFXLightingModel extends LightingModelBase
{

    private ObservableValue<LightGroupSetting> lightGroupSettingObservableValue;
    private final LightGroupSetting backup = new LightGroupSetting("backup");

    private final JavaFXLightInstanceModel[] lightInstanceModels = new JavaFXLightInstanceModel[LightGroupSetting.LIGHT_LIMIT];

    public JavaFXLightingModel(EnvironmentMapModel envModel)
    {
        super(envModel);
        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
        {
            lightInstanceModels[i] = new JavaFXLightInstanceModel();

            lightInstanceModels[i].setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));
        }
    }

    @Override
    public LightInstanceModel getLight(int index)
    {
        return this.lightInstanceModels[index];
    }

    @Override
    public void setLightColor(int i, Vector3 lightColor)
    {
        this.lightInstanceModels[i].setColor(lightColor);
    }

    @Override
    public void setLightMatrix(int i, Matrix4 lightMatrix)
    {
        this.lightInstanceModels[i].setLookMatrix(lightMatrix);
    }

    @Override
    public void setLightCenter(int i, Vector3 lightCenter)
    {
        this.lightInstanceModels[i].setCenter(lightCenter);
    }

    public void setLightGroupSettingObservableValue(ObservableValue<LightGroupSetting> lightGroupSettingObservableValue)
    {
        this.lightGroupSettingObservableValue = lightGroupSettingObservableValue;

        this.lightGroupSettingObservableValue.addListener((observable, oldValue, newValue) ->
        {
            if (newValue != null)
            {
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
                {
                    lightInstanceModels[i].setSubLightSettingObservableValue(newValue.lightListProperty().valueAt(i));
                }
            }
            else
            {
                System.out.println("Binding Backup");
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
                {
                    lightInstanceModels[i].setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));
                }
            }
        });
    }

    private LightGroupSetting lightGroup()
    {
        if (lightGroupSettingObservableValue == null || lightGroupSettingObservableValue.getValue() == null)
        {
            return backup;
        }
        else
        {
            return lightGroupSettingObservableValue.getValue();
        }
    }

    @Override
    public int getLightCount()
    {
        return lightGroup().getNLights();
    }

    @Override
    public boolean isLightVisualizationEnabled(int index)
    {
        return true;
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        return true;
    }

    @Override
    public Vector3 getLightColor(int i)
    {
        return lightInstanceModels[i].getColor();
    }

    @Override
    public Matrix4 getLightMatrix(int i)
    {
        return lightInstanceModels[i].getLookMatrix();
    }

    @Override
    public Vector3 getLightCenter(int i)
    {
        return lightInstanceModels[i].getCenter();
    }
}
