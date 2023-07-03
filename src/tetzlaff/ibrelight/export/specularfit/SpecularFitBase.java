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
    private final BasisResources<ContextType> basisResources;
    private final BasisWeightResources<ContextType> basisWeightResources;

    private final RoughnessOptimization<ContextType> roughnessOptimization;

    protected SpecularFitBase(ContextType context, TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings) throws FileNotFoundException
    {
        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        basisResources = new BasisResources<>(context, specularBasisSettings);
        basisWeightResources = new BasisWeightResources<>(context, textureFitSettings.width, textureFitSettings.height, specularBasisSettings.getBasisCount());

        // Specular roughness / reflectivity module that manages its own resources
        roughnessOptimization =
            new RoughnessOptimizationSimple<>(basisResources, basisWeightResources, textureFitSettings);
            //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
        roughnessOptimization.clear();
    }

    @Override
    public void close()
    {
        basisResources.close();
        basisWeightResources.close();
        roughnessOptimization.close();
    }

    @Override
    public final Texture2D<ContextType> getSpecularReflectivityMap()
    {
        return roughnessOptimization.getReflectivityTexture();
    }

    @Override
    public final Texture2D<ContextType> getSpecularRoughnessMap()
    {
        return roughnessOptimization.getRoughnessTexture();
    }

    /**
     * Basis functions (originally calculated on the CPU)
     */
    @Override
    public final BasisResources<ContextType> getBasisResources()
    {
        return basisResources;
    }

    /**
     * Basis weights (originally calculated on the CPU)
     */
    @Override
    public final BasisWeightResources<ContextType> getBasisWeightResources()
    {
        return basisWeightResources;
    }

    /**
     * Estimated specular reflectivity and roughness
     */
    public final RoughnessOptimization<ContextType> getRoughnessOptimization()
    {
        return roughnessOptimization;
    }
}
