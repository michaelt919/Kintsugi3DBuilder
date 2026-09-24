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

import kintsugi3d.builder.fit.decomposition.MutableBasisResources;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisResources;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextureResourcesBase<ContextType extends Context<ContextType>> extends ReadonlyTextureResourcesBase<ContextType>
    implements TextureResources<ContextType>
{
    protected static final Logger LOG = LoggerFactory.getLogger(TextureResourcesBase.class);

    protected abstract MutableBasisResources<ContextType> getMutableBasisResources();

    @Override
    public final ReadonlyBasisResources<ContextType> getBasisResources()
    {
        return getMutableBasisResources();
    }

    @Override
    public void deleteBasisMaterial(String materialName)
    {
        MutableBasisResources<ContextType> basisResources = getMutableBasisResources();
        if (basisResources != null)
        {
            basisResources.deleteBasisMaterial(materialName);
        }
    }

    @Override
    public void toggleBasisMaterial(String materialName)
    {
        MutableBasisResources<ContextType> basisResources = getMutableBasisResources();
        if (basisResources != null)
        {
            basisResources.toggleBasisMaterial(materialName);
        }
    }
}
