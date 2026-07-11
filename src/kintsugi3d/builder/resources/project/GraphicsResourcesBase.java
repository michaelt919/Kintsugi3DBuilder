/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.IOException;
import java.util.List;

public abstract class GraphicsResourcesBase<ContextType extends Context<ContextType>> implements GraphicsResources<ContextType>
{
    private final GraphicsResourcesCommon<ContextType> commonResources;

    /**
     * Only one instance will be the owner of the shared resources (typicaly created when a project is loaded)
     */
    private final boolean ownerOfSharedResources;

    GraphicsResourcesBase(GraphicsResourcesCommon<ContextType> commonResources, boolean ownerOfSharedResources)
    {
        this.commonResources = commonResources;
        this.ownerOfSharedResources = ownerOfSharedResources;
    }

    GraphicsResourcesCommon<ContextType> getCommonResources()
    {
        return commonResources;
    }

    @Override
    public final ContextType getContext()
    {
        return commonResources.getContext();
    }

    @Override
    public final ViewSet getViewSet()
    {
        return commonResources.getViewSet();
    }

    @Override
    public float getCameraWeight(int index)
    {
        return commonResources.getCameraWeight(index);
    }

    @Override
    public List<Float> getCameraWeights()
    {
        return commonResources.getCameraWeights();
    }

    @Override
    public final GeometryResources<ContextType> getGeometryResources()
    {
        return commonResources.getGeometryResources();
    }

    @Override
    public final TextureResources<ContextType> getTextureResources()
    {
        return commonResources.getTextureResources();
    }

    @Override
    public final LuminanceMapResources<ContextType> getLuminanceMapResources()
    {
        return commonResources.getLuminanceMapResources();
    }

    @Override
    public SingleCalibratedImageResource<ContextType> createSingleImageResource(int viewIndex, ReadonlyLoadOptionsModel loadOptions)
        throws IOException
    {
        return new SingleCalibratedImageResource<>(getContext(), getViewSet(), viewIndex,
            getViewSet().findFullResImageFile(viewIndex), getGeometry(), loadOptions);
    }

    @Override
    public ImageCache<ContextType> cache(ImageCacheSettings settings, ProgressMonitor monitor) throws IOException, UserCancellationException
    {
        settings.setCacheFolderName(getViewSet().getUUID().toString());

        ImageCache<ContextType> cache = new ImageCache<>(this, settings);

        if (!cache.isInitialized())
        {
            cache.initialize(monitor);
        }

        return cache;
    }

    @Override
    public void updateLuminanceMap(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.getViewSet().setLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues);

        commonResources.updateLuminanceMap();
    }

    @Override
    public void clearLuminanceMap()
    {
        this.getViewSet().clearLuminanceEncoding();

        commonResources.updateLuminanceMap();
    }

    @Override
    public void updateLightCalibration(Vector3 lightCalibration)
    {
        for (int i = 0; i < this.getViewSet().getLightCount(); i++)
        {
            this.getViewSet().setLightPosition(i, lightCalibration);
        }

        commonResources.updateLightData();
    }

    @Override
    public void replaceTextureResources(TextureResources<ContextType> textureResources)
    {
        commonResources.replaceTextureResources(textureResources);
    }

    @Override
    public void initializeLightIntensities(Vector3 lightIntensity)
    {
        for (int i = 0; i < this.getViewSet().getLightCount(); i++)
        {
            this.getViewSet().setLightIntensity(i, lightIntensity);
        }

        this.commonResources.updateLightData();
    }

    @Override
    public void close()
    {
        if (this.ownerOfSharedResources && this.commonResources != null)
        {
            this.commonResources.close();
        }
    }
}
