package tetzlaff.ibrelight.javafx.internal;//Created by alexk on 7/25/2017.

import javafx.beans.value.ObservableValue;
import tetzlaff.ibrelight.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.models.EnvironmentModel;
import tetzlaff.models.impl.ExtendedLightingModelBase;

public final class LightingModelImpl extends ExtendedLightingModelBase<LightInstanceModelImpl>
{
    private ObservableValue<LightGroupSetting> selectedLightGroupSetting;
    private final LightGroupSetting sentinel = new LightGroupSetting("sentinel");

    public LightingModelImpl(EnvironmentModel envModel)
    {
        super(LightGroupSetting.LIGHT_LIMIT, i -> new LightInstanceModelImpl(), envModel);
        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
        {
            getLight(i).setSubLightSettingObservableValue(sentinel.lightListProperty().valueAt(i));
        }
    }

    public void setSelectedLightGroupSetting(ObservableValue<LightGroupSetting> selectedLightGroupSetting)
    {
        this.selectedLightGroupSetting = selectedLightGroupSetting;

        this.selectedLightGroupSetting.addListener((observable, oldValue, newValue) ->
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
                    getLight(i).setSubLightSettingObservableValue(sentinel.lightListProperty().valueAt(i));
                }
            }
        });
    }

    private LightGroupSetting getActiveLightGroupSetting()
    {
        if (selectedLightGroupSetting == null || selectedLightGroupSetting.getValue() == null)
        {
            return sentinel;
        }
        else
        {
            return selectedLightGroupSetting.getValue();
        }
    }

    @Override
    public int getLightCount()
    {
        return getActiveLightGroupSetting().getLightCount();
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        LightGroupSetting activeLightGroup = getActiveLightGroupSetting();
        return !activeLightGroup.isLocked() && !activeLightGroup.getLightList().get(index).locked().get();
    }

    @Override
    public int getSelectedLightIndex()
    {
        return getActiveLightGroupSetting().getSelectedLightIndex();
    }

    @Override
    public void setSelectedLightIndex(int index)
    {
        getActiveLightGroupSetting().setSelectedLightIndex(index);
    }
}
