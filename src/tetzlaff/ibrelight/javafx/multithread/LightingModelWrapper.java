/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.ibrelight.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.impl.ExtendedLightingModelBase;

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
