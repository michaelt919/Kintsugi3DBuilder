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

import kintsugi3d.builder.state.scene.DiscreteLightModel;
import kintsugi3d.builder.state.scene.DiscreteLightModelBase;
import kintsugi3d.gl.vecmath.Vector3;

public class SynchronizedDiscreteLightModel extends DiscreteLightModelBase
{
    private final DiscreteLightModel baseModel;
    private final SynchronizedValue<Vector3> color;
    private final SynchronizedValue<Float> spotSize;
    private final SynchronizedValue<Float> spotTaper;

    public SynchronizedDiscreteLightModel(DiscreteLightModel baseModel)
    {
        super(new SynchronizedCameraModel(baseModel));
        this.baseModel = baseModel;

        this.color = SynchronizedValue.createFromFunctions(baseModel::getColor, baseModel::setColor);
        this.spotSize = SynchronizedValue.createFromFunctions(baseModel::getSpotSize, baseModel::setSpotSize);
        this.spotTaper = SynchronizedValue.createFromFunctions(baseModel::getSpotTaper, baseModel::setSpotTaper);
    }

    @Override
    public boolean isEnabled()
    {
        return this.baseModel.isEnabled();
    }

    @Override
    public Vector3 getColor()
    {
        return color.getValue();
    }

    @Override
    public float getSpotSize()
    {
        return spotSize.getValue();
    }

    @Override
    public float getSpotTaper()
    {
        return spotTaper.getValue();
    }

    @Override
    public void setColor(Vector3 color)
    {
        this.color.setValue(color);
    }

    @Override
    public void setSpotSize(float spotSize)
    {
        this.spotSize.setValue(spotSize);
    }

    @Override
    public void setSpotTaper(float spotTaper)
    {
        this.spotTaper.setValue(spotTaper);
    }
}
