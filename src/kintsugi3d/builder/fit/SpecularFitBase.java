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

package kintsugi3d.builder.fit;

import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.fit.roughness.RoughnessOptimization;
import kintsugi3d.builder.fit.roughness.RoughnessOptimizationSimple;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.specular.SpecularMaterialResourcesBase;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class SpecularFitBase<ContextType extends Context<ContextType>>
    extends SpecularMaterialResourcesBase<ContextType>
{
    private final ContextType context;
    private final BasisResources<ContextType> basisResources;
    private final BasisWeightResources<ContextType> basisWeightResources;
    private final boolean basisResourcesOwned;

    private final RoughnessOptimization<ContextType> roughnessOptimization;

    /**
     *
     * @param basisResources
     * @param basisResourcesOwned If false, basis resources will not be managed / owned by this instance and will never be destroyed by this instance.
     *                            Basis weight resources, however, will always be managed / owned and destroyed when this instance is closed.
     * @param textureResolution
     * @throws FileNotFoundException
     */
    protected SpecularFitBase(BasisResources<ContextType> basisResources, boolean basisResourcesOwned,
        TextureResolution textureResolution) throws IOException
    {
        this.context = basisResources.getContext();

        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        this.basisResources = basisResources;
        this.basisResourcesOwned = basisResourcesOwned;
        this.basisWeightResources = new BasisWeightResources<>(basisResources.getContext(),
            textureResolution.width, textureResolution.height, basisResources.getBasisCount());

        // Specular roughness / reflectivity module that manages its own resources
        this.roughnessOptimization =
            new RoughnessOptimizationSimple<>(basisResources, basisWeightResources, textureResolution);
        //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
        this.roughnessOptimization.clear();
    }

    /**
     * Basis resources and basis weight resources will be managed / owned by this instance
     * @param context
     * @param textureResolution
     * @param specularBasisSettings
     * @throws FileNotFoundException
     */
    protected SpecularFitBase(ContextType context, TextureResolution textureResolution,
        SpecularBasisSettings specularBasisSettings) throws IOException
    {
        this(new BasisResources<>(context, specularBasisSettings.getBasisCount(), specularBasisSettings.getBasisResolution()),
            true, textureResolution);
    }

    /**
     * Basis resources and basis weight resources will be managed / owned by this instance
     * Roughness and reflectivity textures will be loaded from prior solution
     * @return
     */
    protected SpecularFitBase(ContextType context, File priorSolutionDirectory, ProgressMonitor monitor) throws IOException {
        this.context = context;

        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        this.basisResources = BasisResources.loadFromPriorSolution(context, priorSolutionDirectory, monitor);
        this.basisResourcesOwned = true;

        if (this.basisResources != null)
        {
            // Specular roughness / reflectivity module that manages its own resources
            this.roughnessOptimization =
                new RoughnessOptimizationSimple<>(basisResources, priorSolutionDirectory);
            //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
            // Don't clear it since the roughness and specular textures will be storing the images loaded from disk

            this.basisWeightResources = BasisWeightResources.loadFromPriorSolution(
                context, priorSolutionDirectory,
                roughnessOptimization.getRoughnessTexture().getWidth(), roughnessOptimization.getRoughnessTexture().getHeight(),
                basisResources.getBasisCount(), monitor);

            this.roughnessOptimization.setInputWeights(basisWeightResources);
        }
        else
        {
            roughnessOptimization = null;
            basisWeightResources = null;
        }
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public int getWidth()
    {
        return basisWeightResources == null || basisWeightResources.weightMaps == null ? 0 : basisWeightResources.weightMaps.getWidth();
    }

    @Override
    public int getHeight()
    {
        return basisWeightResources == null || basisWeightResources.weightMaps == null ? 0 : basisWeightResources.weightMaps.getHeight();
    }

    @Override
    public void close()
    {
        if (basisResourcesOwned && basisResources != null)
        {
            basisResources.close();
        }

        if (basisWeightResources != null)
        {
            basisWeightResources.close();
        }

        if (roughnessOptimization != null)
        {
            roughnessOptimization.close();
        }
    }

    @Override
    public final Texture2D<ContextType> getSpecularReflectivityMap()
    {
        return roughnessOptimization == null ? null : roughnessOptimization.getReflectivityTexture();
    }

    @Override
    public final Texture2D<ContextType> getSpecularRoughnessMap()
    {
        return roughnessOptimization == null ? null : roughnessOptimization.getRoughnessTexture();
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
