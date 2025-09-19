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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.gl.material.MaterialResources;

import java.util.function.Supplier;

public class GenericMaterialResources<ContextType extends Context<ContextType>>
    extends SpecularMaterialResourcesBase<ContextType>
{
    private final MaterialResources<ContextType> baseResources;

    private GenericMaterialResources(Supplier<MaterialResources<ContextType>> resourcesFactory)
    {
        this.baseResources = resourcesFactory.get();
    }

    public static <ContextType extends Context<ContextType>> GenericMaterialResources<ContextType> wrap(
        Supplier<MaterialResources<ContextType>> resourcesFactory)
    {
        return new GenericMaterialResources<>(resourcesFactory);
    }

    public static <ContextType extends Context<ContextType>> GenericMaterialResources<ContextType> createNull()
    {
        return new GenericMaterialResources<>(MaterialResources::createNull);
    }

    @Override
    public ContextType getContext()
    {
        return baseResources.getContext();
    }

    @Override
    public int getWidth()
    {
        return baseResources.getDiffuseTexture() == null ? 0 : baseResources.getDiffuseTexture().getWidth();
    }

    @Override
    public int getHeight()
    {
        return baseResources.getDiffuseTexture() == null ? 0 : baseResources.getDiffuseTexture().getHeight();
    }

    @Override
    public Texture2D<ContextType> getDiffuseMap()
    {
        return baseResources.getDiffuseTexture();
    }

    @Override
    public Texture2D<ContextType> getNormalMap()
    {
        return baseResources.getNormalTexture();
    }

    @Override
    public Texture2D<ContextType> getConstantMap()
    {
        return null;
    }

    @Override
    public Texture2D<ContextType> getSpecularReflectivityMap()
    {
        return baseResources.getSpecularReflectivityTexture();
    }

    @Override
    public Texture2D<ContextType> getSpecularRoughnessMap()
    {
        return baseResources.getSpecularRoughnessTexture();
    }

    @Override
    public Texture2D<ContextType> getAlbedoMap()
    {
        // diffuse map from photogrammetry can also be used as an albedo map, if necessary
        return baseResources.getDiffuseTexture();
    }

    @Override
    public Texture2D<ContextType> getORMMap()
    {
        return null;
    }

    @Override
    public Texture2D<ContextType> getOcclusionMap()
    {
        return baseResources.getOcclusionTexture();
    }

    @Override
    public BasisResources<ContextType> getBasisResources()
    {
        return null;
    }

    @Override
    public BasisWeightResources<ContextType> getBasisWeightResources()
    {
        return null;
    }

    @Override
    public void close()
    {
        baseResources.close();
    }
}
