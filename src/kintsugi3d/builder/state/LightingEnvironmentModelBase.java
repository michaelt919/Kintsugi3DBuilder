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

package kintsugi3d.builder.state;

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public abstract class LightingEnvironmentModelBase<DiscreteLightType extends DiscreteLightModel> implements ManipulableLightingEnvironmentModel
{
    private boolean lightWidgetsEthereal;
    private final List<DiscreteLightType> lightInstanceModels;
    private final LightWidgetModel[] lightWidgetModels;
    private final EnvironmentModel environmentModel;

    protected LightingEnvironmentModelBase(int lightCount, IntFunction<DiscreteLightType> lightInstanceCreator, EnvironmentModel environmentModel)
    {
        this.lightInstanceModels = new ArrayList<>(lightCount);
        this.lightWidgetModels = new LightWidgetModel[lightCount];
        for (int i = 0; i < lightCount; i++)
        {
            lightInstanceModels.add(lightInstanceCreator.apply(i));
            lightWidgetModels[i] = new LightWidgetModel();
        }

        this.environmentModel = environmentModel;
    }

    @Override
    public float getBackgroundIntensity()
    {
        return environmentModel.getBackgroundIntensity();
    }

    @Override
    public void setBackgroundIntensity(float backgroundIntensity)
    {
        environmentModel.setBackgroundIntensity(backgroundIntensity);
    }

    @Override
    public final Vector3 getBackgroundColor()
    {
        return environmentModel.getBackgroundColor();
    }

    @Override
    public BackgroundMode getBackgroundMode()
    {
        return environmentModel.getBackgroundMode();
    }

    @Override
    public Vector3 getGroundPlaneColor()
    {
        return environmentModel.getGroundPlaneColor();
    }

    @Override
    public boolean isGroundPlaneEnabled()
    {
        return environmentModel.isGroundPlaneEnabled();
    }

    @Override
    public float getGroundPlaneHeight()
    {
        return environmentModel.getGroundPlaneHeight();
    }

    @Override
    public float getGroundPlaneSize()
    {
        return environmentModel.getGroundPlaneSize();
    }

    @Override
    public float getAmbientLightIntensity()
    {
        return environmentModel.getEnvironmentIntensity();
    }

    @Override
    public void setAmbientLightIntensity(float environmentIntensity)
    {
        environmentModel.setEnvironmentIntensity(environmentIntensity);
    }

    @Override
    public final Vector3 getAmbientLightColor() 
    {
        return environmentModel.getEnvironmentColor();
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return environmentModel.isEnvironmentMappingEnabled();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() 
    {
        return environmentModel.getEnvironmentMapMatrix();
    }

    @Override
    public float getEnvironmentMapFilteringBias()
    {
        return environmentModel.getEnvironmentMapFilteringBias();
    }

    @Override
    public boolean areLightWidgetsEthereal()
    {
        return this.lightWidgetsEthereal;
    }

    @Override
    public void setLightWidgetsEthereal(boolean lightWidgetsEthereal)
    {
        this.lightWidgetsEthereal = lightWidgetsEthereal;
    }

    @Override
    public LightWidgetModel getLightWidgetModel(int index)
    {
        return this.lightWidgetModels[index];
    }

    @Override
    public boolean isLightVisualizationEnabled(int index)
    {
        return true;
    }

    @Override
    public LightPrototypeModel getLightPrototype(int i)
    {
        return lightInstanceModels.get(i);
    }

    @Override
    public Matrix4 getLightMatrix(int i)
    {
        return lightInstanceModels.get(i).getLookMatrix();
    }

    @Override
    public Vector3 getLightCenter(int i)
    {
        return lightInstanceModels.get(i).getTarget();
    }


    @Override
    public DiscreteLightType getLight(int index)
    {
        return this.lightInstanceModels.get(index);
    }

    @Override
    public void setLightMatrix(int i, Matrix4 lightMatrix)
    {
        if (this.isLightWidgetEnabled(i))
        {
            this.lightInstanceModels.get(i).setLookMatrix(lightMatrix);
        }
    }

    @Override
    public void setLightCenter(int i, Vector3 lightCenter)
    {
        if (this.isLightWidgetEnabled(i))
        {
            this.lightInstanceModels.get(i).setTarget(lightCenter);
        }
    }

    @Override
    public EnvironmentModel getEnvironmentModel()
    {
        return this.environmentModel;
    }
}
