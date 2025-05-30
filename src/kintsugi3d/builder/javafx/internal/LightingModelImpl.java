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

package kintsugi3d.builder.javafx.internal;//Created by alexk on 7/25/2017.

import javafx.beans.value.ObservableValue;
import kintsugi3d.builder.javafx.controllers.scene.lights.LightGroupSetting;
import kintsugi3d.builder.state.EnvironmentModel;
import kintsugi3d.builder.state.impl.ExtendedLightingModelBase;

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
    public int getMaxLightCount()
    {
        return 4;
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
