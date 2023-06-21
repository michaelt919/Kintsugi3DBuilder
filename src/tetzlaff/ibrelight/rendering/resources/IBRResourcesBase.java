/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.ibrelight.rendering.resources;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.material.MaterialResources;
import tetzlaff.ibrelight.core.ViewSet;

import java.util.List;

public abstract class IBRResourcesBase<ContextType extends Context<ContextType>> implements IBRResources<ContextType>
{
    private IBRSharedResources<ContextType> sharedResources;

    /**
     * Only one instance will be the owner of the shared resources (typicaly created when a project is loaded)
     */
    private final boolean ownerOfSharedResources;

    protected IBRResourcesBase(IBRSharedResources<ContextType> sharedResources, boolean ownerOfSharedResources)
    {
        this.sharedResources = sharedResources;
        this.ownerOfSharedResources = ownerOfSharedResources;
    }

    protected IBRSharedResources<ContextType> getSharedResources()
    {
        return sharedResources;
    }

    @Override
    public ContextType getContext()
    {
        return sharedResources.getContext();
    }

    @Override
    public ViewSet getViewSet()
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
    public MaterialResources<ContextType> getMaterialResources()
    {
        return sharedResources.getMaterialResources();
    }

    @Override
    public LuminanceMapResources<ContextType> getLuminanceMapResources()
    {
        return sharedResources.getLuminanceMapResources();
    }

    @Override
    public void updateLuminanceMap()
    {
        sharedResources.updateLuminanceMap();
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
