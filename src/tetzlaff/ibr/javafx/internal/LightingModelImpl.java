package tetzlaff.ibr.javafx.internal;//Created by alexk on 7/25/2017.

import javafx.beans.value.ObservableValue;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.models.EnvironmentModel;
import tetzlaff.models.impl.ExtendedLightingModelBase;

public class LightingModelImpl extends ExtendedLightingModelBase<LightInstanceModelImpl>
{
    private ObservableValue<LightGroupSetting> lightGroupSettingObservableValue;
    private final LightGroupSetting backup = new LightGroupSetting("backup");

    public LightingModelImpl(EnvironmentModel envModel)
    {
        super(LightGroupSetting.LIGHT_LIMIT, i -> new LightInstanceModelImpl(), envModel);
        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
        {
            getLight(i).setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));
        }
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
                    getLight(i).setSubLightSettingObservableValue(newValue.lightListProperty().valueAt(i));
                }
            }
            else
            {
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
                {
                    getLight(i).setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));
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
}
