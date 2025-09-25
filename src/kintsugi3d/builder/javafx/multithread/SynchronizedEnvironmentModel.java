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

import kintsugi3d.builder.state.scene.BackgroundMode;
import kintsugi3d.builder.state.scene.EnvironmentModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public class SynchronizedEnvironmentModel implements EnvironmentModel
{
    private final EnvironmentModel baseModel;
    private final SynchronizedValue<Float> environmentRotation;
    private final SynchronizedValue<Float> environmentIntensity;
    private final SynchronizedValue<Float> backgroundIntensity;

    public SynchronizedEnvironmentModel(EnvironmentModel baseModel)
    {
        this.baseModel = baseModel;
        this.environmentRotation = SynchronizedValue.createFromFunctions(baseModel::getEnvironmentRotation, baseModel::setEnvironmentRotation);
        this.environmentIntensity = SynchronizedValue.createFromFunctions(baseModel::getEnvironmentIntensity, baseModel::setEnvironmentIntensity);
        this.backgroundIntensity = SynchronizedValue.createFromFunctions(baseModel::getBackgroundIntensity, baseModel::setBackgroundIntensity);
    }

    @Override
    public boolean isEnabled()
    {
        return baseModel.isEnabled();
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return baseModel.isEnvironmentMappingEnabled();
    }

    @Override
    public Vector3 getEnvironmentColor()
    {
        return baseModel.getEnvironmentColor();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix()
    {
        return baseModel.getEnvironmentMapMatrix();
    }

    @Override
    public float getEnvironmentMapFilteringBias()
    {
        return baseModel.getEnvironmentMapFilteringBias();
    }

    @Override
    public BackgroundMode getBackgroundMode()
    {
        return baseModel.getBackgroundMode();
    }

    @Override
    public Vector3 getBackgroundColor()
    {
        return baseModel.getBackgroundColor();
    }

    @Override
    public float getEnvironmentRotation()
    {
        return this.environmentRotation.getValue();
    }

    @Override
    public float getEnvironmentIntensity()
    {
        return this.environmentIntensity.getValue();
    }

    @Override
    public float getBackgroundIntensity()
    {
        return this.backgroundIntensity.getValue();
    }

    @Override
    public void setEnvironmentRotation(float environmentRotation)
    {
        this.environmentRotation.setValue(environmentRotation);
    }

    @Override
    public void setEnvironmentIntensity(float environmentIntensity)
    {
        this.environmentIntensity.setValue(environmentIntensity);
    }

    @Override
    public void setBackgroundIntensity(float backgroundIntensity)
    {
        this.backgroundIntensity.setValue(backgroundIntensity);
    }

    @Override
    public boolean isGroundPlaneEnabled()
    {
        return baseModel.isGroundPlaneEnabled();
    }

    @Override
    public Vector3 getGroundPlaneColor()
    {
        return baseModel.getGroundPlaneColor();
    }

    @Override
    public float getGroundPlaneHeight()
    {
        return baseModel.getGroundPlaneHeight();
    }

    @Override
    public float getGroundPlaneSize()
    {
        return baseModel.getGroundPlaneSize();
    }
}
