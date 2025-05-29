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

package kintsugi3d.builder.javafx.multithread;

import kintsugi3d.builder.javafx.controllers.scene.lights.LightGroupSetting;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.state.ExtendedLightingModel;
import kintsugi3d.builder.state.impl.ExtendedLightingModelBase;

public class LightingModelWrapper extends ExtendedLightingModelBase<LightInstanceModelWrapper>
{
    private final ExtendedLightingModel baseModel;
    private final MultithreadValue<Integer> selectedLightIndex;

    public LightingModelWrapper(ExtendedLightingModel baseModel)
    {
        super(LightGroupSetting.LIGHT_LIMIT,
            index -> new LightInstanceModelWrapper(baseModel.getLight(index)),
            new EnvironmentModelWrapper(baseModel.getEnvironmentModel()));
        this.baseModel = baseModel;
        this.selectedLightIndex = MultithreadValue.createFromFunctions(baseModel::getSelectedLightIndex, baseModel::setSelectedLightIndex);
    }

    @Override
    public int getLightCount()
    {
        return baseModel.getLightCount();
    }

    @Override
    public int getMaxLightCount()
    {
        return baseModel.getMaxLightCount();
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        return baseModel.isLightWidgetEnabled(index);
    }

    @Override
    public int getSelectedLightIndex()
    {
        return selectedLightIndex.getValue();
    }

    @Override
    public void setSelectedLightIndex(int index)
    {
        this.selectedLightIndex.setValue(index);
    }
}
