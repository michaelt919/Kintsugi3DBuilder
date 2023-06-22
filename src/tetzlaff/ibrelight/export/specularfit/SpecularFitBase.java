/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.FileNotFoundException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture2D;
import tetzlaff.ibrelight.core.TextureFitSettings;

public abstract class SpecularFitBase<ContextType extends Context<ContextType>> implements SpecularResources<ContextType>
{
    /**
     * Basis functions and weights (originally calculated on the CPU)
     */
    final BasisResources<ContextType> basisResources;

    /**
     * Estimated specular reflectivity and roughness
     */
    final RoughnessOptimization<ContextType> roughnessOptimization;

    protected SpecularFitBase(ContextType context, TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings) throws FileNotFoundException
    {
        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        basisResources = new BasisResources<>(context, textureFitSettings, specularBasisSettings);

        // Specular roughness / reflectivity module that manages its own resources
        roughnessOptimization =
            new RoughnessOptimizationSimple<>(context, basisResources, textureFitSettings);
            //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
        roughnessOptimization.clear();
    }

    @Override
    public void close()
    {
        basisResources.close();
        roughnessOptimization.close();
    }

    @Override
    public Texture2D<ContextType> getSpecularReflectivityMap()
    {
        return roughnessOptimization.getReflectivityTexture();
    }

    @Override
    public Texture2D<ContextType> getSpecularRoughnessMap()
    {
        return roughnessOptimization.getRoughnessTexture();
    }

    @Override
    public BasisResources<ContextType> getBasisResources()
    {
        return basisResources;
    }
}
