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

package kintsugi3d.builder.fit;

import kintsugi3d.builder.core.texture.StandardTexture;
import kintsugi3d.builder.core.texture.TextureInfo;
import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisResources;
import kintsugi3d.builder.fit.roughness.RoughnessOptimization;
import kintsugi3d.builder.fit.roughness.RoughnessOptimizationSimple;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResourcesBase;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.ManagedResource;
import kintsugi3d.gl.core.Texture2D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public abstract class SpecularFitBase<ContextType extends Context<ContextType>>
    extends ReadonlyTextureResourcesBase<ContextType> implements ManagedResource
{
    private final ContextType context;
    private final BasisResources<ContextType> basisResources;
    private final BasisWeightResources<ContextType> basisWeightResources;
    private final RoughnessOptimization<ContextType> roughnessOptimization;

    /**
     *
     * @param basisResources
     * @param roughnessTexResolution
     * @throws FileNotFoundException
     */
    protected SpecularFitBase(
        ContextType context, BasisResources<ContextType> basisResources,
        BasisWeightResources<ContextType> basisWeightResources, TextureResolution roughnessTexResolution)
        throws IOException
    {
        this.context = context;

        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        this.basisResources = basisResources;
        this.basisWeightResources = basisWeightResources;

        if (basisResources != null)
        {
            // Specular roughness / reflectivity module that manages its own resources
            this.roughnessOptimization =
                new RoughnessOptimizationSimple<>(basisResources, basisWeightResources, roughnessTexResolution);
            //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
            this.roughnessOptimization.clear();
        }
        else
        {
            this.roughnessOptimization = null;
        }
    }

    /**
     * Basis resources and basis weight resources will be managed / owned by this instance
     * Roughness and reflectivity textures will be loaded from prior solution
     * @return
     */
    protected SpecularFitBase(
        ContextType context, BasisResources<ContextType> basisResources,
        BasisWeightResources<ContextType> basisWeightResources, File priorSolutionDirectory)
        throws IOException
    {
        this.context = context;
        this.basisResources = basisResources;
        this.basisWeightResources = basisWeightResources;

        if (basisResources != null)
        {
            // Specular roughness / reflectivity module that manages its own resources
            this.roughnessOptimization =
                new RoughnessOptimizationSimple<>(basisResources, basisWeightResources, priorSolutionDirectory);
            //new RoughnessOptimizationIterative<>(context, basisResources, this::getDiffuseMap, settings);
            // Don't clear it since the roughness and specular textures will be storing the images loaded from disk
        }
        else
        {
            roughnessOptimization = null;
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
        return basisWeightResources == null || basisWeightResources.getWeightMaps() == null ?
            0 : basisWeightResources.getWeightMaps().getWidth();
    }

    @Override
    public int getHeight()
    {
        return basisWeightResources == null || basisWeightResources.getWeightMaps() == null ?
            0 : basisWeightResources.getWeightMaps().getHeight();
    }

    @Override
    public void close()
    {
        if (basisResources != null)
        {
            basisResources.close();
        }

        if (roughnessOptimization != null)
        {
            roughnessOptimization.close();
        }
    }

    protected int getSpecularTextureCount()
    {
        //noinspection VariableNotUsedInsideIf
        return (roughnessOptimization == null) ? 0 : 2;
    }

    protected Map<StandardTexture, Texture2D<ContextType>> getStandardSpecularTextures()
    {
        return roughnessOptimization == null ? Map.of() :
            Map.of(StandardTexture.SPECULAR_COLOR, roughnessOptimization.getReflectivityTexture(),
                StandardTexture.ROUGHNESS, roughnessOptimization.getRoughnessTexture());
    }

    protected Map<TextureInfo, Texture2D<ContextType>> getSpecularTextures()
    {
        //noinspection VariableNotUsedInsideIf
        return roughnessOptimization == null ? Map.of() : StandardTexture.convertEnumMapToObjectMap(getStandardSpecularTextures());
    }

    /**
     * Estimated specular reflectivity and roughness
     */
    public final RoughnessOptimization<ContextType> getRoughnessOptimization()
    {
        return roughnessOptimization;
    }

    @Override
    public ReadonlyBasisResources<ContextType> getBasisResources()
    {
        return basisResources;
    }

    /**
     * Basis weights (originally calculated on the CPU)
     */
    @Override
    public BasisWeightResources<ContextType> getBasisWeightResources()
    {
        return basisWeightResources;
    }
}
