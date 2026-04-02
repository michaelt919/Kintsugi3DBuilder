/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.scene;

import kintsugi3d.builder.state.project.LightGroupSettings;

import java.util.function.IntFunction;

public abstract class LightingEnvironmentModelFromSettings<LightType extends DiscreteLightModel> extends LightingEnvironmentModelBase<LightType>
{
    public LightingEnvironmentModelFromSettings(int lightCount, IntFunction<LightType> lightInstanceCreator, EnvironmentModel environmentModel)
    {
        super(lightCount, lightInstanceCreator, environmentModel);
    }

    protected abstract LightGroupSettings<?> getLightGroupSetting();

    @Override
    public int getLightCount()
    {
        return getLightGroupSetting().getLightCount();
    }

    @Override
    public int getMaxLightCount()
    {
        return 4;
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        LightGroupSettings<?> activeLightGroup = getLightGroupSetting();
        return !activeLightGroup.isLocked() && !activeLightGroup.getLightList().get(index).isLocked();
    }
}
