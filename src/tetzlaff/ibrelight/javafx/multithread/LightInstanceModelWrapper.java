/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.LightInstanceModel;
import tetzlaff.models.impl.LightInstanceModelBase;

public class LightInstanceModelWrapper extends LightInstanceModelBase
{
    private final LightInstanceModel baseModel;
    private final MultithreadValue<Vector3> color;
    private final MultithreadValue<Float> spotSize;
    private final MultithreadValue<Float> spotTaper;

    public LightInstanceModelWrapper(LightInstanceModel baseModel)
    {
        super(new CameraModelWrapper(baseModel));
        this.baseModel = baseModel;

        this.color = MultithreadValue.createFromFunctions(baseModel::getColor, baseModel::setColor);
        this.spotSize = MultithreadValue.createFromFunctions(baseModel::getSpotSize, baseModel::setSpotSize);
        this.spotTaper = MultithreadValue.createFromFunctions(baseModel::getSpotTaper, baseModel::setSpotTaper);
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
