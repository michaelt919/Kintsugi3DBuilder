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
    private final boolean basisResourcesOwned;

    private final RoughnessOptimization<ContextType> roughnessOptimization;

    /**
     *
     * @param basisResources
     * @param basisResourcesOwned If false, basis resources will not be managed / owned by this instance and will never be destroyed by this instance.
     *                            Basis weight resources, however, will always be managed / owned and destroyed when this instance is closed.
     * @param textureFitSettings
     * @throws FileNotFoundException
     */
    protected SpecularFitBase(BasisResources<ContextType> basisResources, boolean basisResourcesOwned,
        TextureFitSettings textureFitSettings) throws FileNotFoundException
    {
        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        this.basisResources = basisResources;
        this.basisResourcesOwned = basisResourcesOwned;
        this.basisWeightResources = new BasisWeightResources<>(basisResources.getContext(),
            textureFitSettings.width, textureFitSettings.height, basisResources.getSpecularBasisSettings().getBasisCount());

        // Specular roughness / reflectivity module that manages its own resources
        this.roughnessOptimization =
            new RoughnessOptimizationSimple<>(basisResources, basisWeightResources, textureFitSettings);
        //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
        this.roughnessOptimization.clear();
    }

    /**
     * Basis resources and basis weight resources will be managed / owned by this instance
     * @param context
     * @param textureFitSettings
     * @param specularBasisSettings
     * @throws FileNotFoundException
     */
    protected SpecularFitBase(ContextType context, TextureFitSettings textureFitSettings,
        SpecularBasisSettings specularBasisSettings) throws FileNotFoundException
    {
        this(new BasisResources<>(context, specularBasisSettings), true, textureFitSettings);
    }

    @Override
    public void close()
    {
        if (basisResourcesOwned)
        {
            basisResources.close();
        }

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
