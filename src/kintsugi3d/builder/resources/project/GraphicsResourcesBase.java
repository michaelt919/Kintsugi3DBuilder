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

package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.vecmath.Vector3;

import java.util.List;

public abstract class GraphicsResourcesBase<ContextType extends Context<ContextType>> implements GraphicsResources<ContextType>
{
    private GraphicsResourcesCommon<ContextType> sharedResources;

    /**
     * Only one instance will be the owner of the shared resources (typicaly created when a project is loaded)
     */
    private final boolean ownerOfSharedResources;

    GraphicsResourcesBase(GraphicsResourcesCommon<ContextType> sharedResources, boolean ownerOfSharedResources)
    {
        this.sharedResources = sharedResources;
        this.ownerOfSharedResources = ownerOfSharedResources;
    }

    GraphicsResourcesCommon<ContextType> getSharedResources()
    {
        return sharedResources;
    }

    @Override
    public final ContextType getContext()
    {
        return sharedResources.getContext();
    }

    @Override
    public final ViewSet getViewSet()
    {
        return sharedResources.getViewSet();
    }

    @Override
    public float getCameraWeight(int index)
    {
        return sharedResources.getCameraWeight(index);
    }

    @Override
    public List<Float> getCameraWeights()
    {
        return sharedResources.getCameraWeights();
    }

    @Override
    public final GeometryResources<ContextType> getGeometryResources()
    {
        return sharedResources.getGeometryResources();
    }

    @Override
    public final SpecularMaterialResources<ContextType> getSpecularMaterialResources()
    {
        return sharedResources.getSpecularMaterialResources();
    }

    @Override
    public final LuminanceMapResources<ContextType> getLuminanceMapResources()
    {
        return sharedResources.getLuminanceMapResources();
    }

    @Override
    public void updateLuminanceMap(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.getViewSet().setTonemapping(
            linearLuminanceValues,
            encodedLuminanceValues);

        sharedResources.updateLuminanceMap();
    }

    @Override
    public void updateLightCalibration(Vector3 lightCalibration)
    {
        for (int i = 0; i < this.getViewSet().getLightCount(); i++)
        {
            this.getViewSet().setLightPosition(i, lightCalibration);
        }

        sharedResources.updateLightData();
    }

    @Override
    public void replaceSpecularMaterialResources(SpecularMaterialResources<ContextType> specularMaterialResources)
    {
        sharedResources.replaceSpecularMaterialResources(specularMaterialResources);
    }

    @Override
    public void initializeLightIntensities(Vector3 lightIntensity, boolean infiniteLightSources)
    {
        for (int i = 0; i < this.getViewSet().getLightCount(); i++)
        {
            this.getViewSet().setLightIntensity(i, lightIntensity);
        }

        this.getViewSet().setInfiniteLightSources(infiniteLightSources);
        this.sharedResources.updateLightData();
    }

    @Override
    public void close()
    {
        if (this.ownerOfSharedResources && this.sharedResources != null)
        {
            this.sharedResources.close();
            this.sharedResources = null;
        }
    }
}
