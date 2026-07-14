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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.core.TextureDetails;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.gl.material.ImportedMaterialResources;

import java.util.Map;
import java.util.function.Supplier;

public final class ImportedMaterialResourcesWrapper<ContextType extends Context<ContextType>>
    extends TextureResourcesBase<ContextType>
{
    private final ImportedMaterialResources<ContextType> importedResources;

    private ImportedMaterialResourcesWrapper(Supplier<ImportedMaterialResources<ContextType>> resourcesFactory)
    {
        this.importedResources = resourcesFactory.get();
    }

    public static <ContextType extends Context<ContextType>> ImportedMaterialResourcesWrapper<ContextType> wrap(
        Supplier<ImportedMaterialResources<ContextType>> resourcesFactory)
    {
        return new ImportedMaterialResourcesWrapper<>(resourcesFactory);
    }

    public static <ContextType extends Context<ContextType>> ImportedMaterialResourcesWrapper<ContextType> createNull()
    {
        return new ImportedMaterialResourcesWrapper<>(ImportedMaterialResources::createNull);
    }

    @Override
    public ContextType getContext()
    {
        return importedResources.getContext();
    }

    @Override
    public int getWidth()
    {
        Texture2D<ContextType> diffuseTex = getTexture(StandardTexture.DIFFUSE_COLOR);
        return diffuseTex == null ? 0 : diffuseTex.getWidth();
    }

    @Override
    public int getHeight()
    {
        Texture2D<ContextType> diffuseTex = getTexture(StandardTexture.DIFFUSE_COLOR);
        return diffuseTex == null ? 0 : diffuseTex.getHeight();
    }

    @Override
    public Map<TextureDetails, Texture2D<ContextType>> getTextures()
    {
        return StandardTexture.convertEnumMapToObjectMap(getStandardTextures());
    }

    @Override
    public Map<StandardTexture, Texture2D<ContextType>> getStandardTextures()
    {
        return importedResources.getTextures();
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
        importedResources.close();
    }
}
