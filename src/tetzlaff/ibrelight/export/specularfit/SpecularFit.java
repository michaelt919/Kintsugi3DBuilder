/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
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
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Texture2D;
import tetzlaff.gl.core.Texture3D;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

/**
 * A class that bundles all of the GPU resources for representing a final specular fit solution.
 * @param <ContextType>
 */
@SuppressWarnings("PackageVisibleField")
public class SpecularFit<ContextType extends Context<ContextType>> implements SpecularResources<ContextType>
{
    /**
     * Basis functions and weights (originally calculated on the CPU)
     */
    final BasisResources<ContextType> basisResources;

    /**
     * Estimated surface normals
     */
    final NormalOptimization<ContextType> normalOptimization;

    /**
     * Estimated specular reflectivity and roughness
     */
    final RoughnessOptimization<ContextType> roughnessOptimization;

    /**
     * Final diffuse estimate
     */
    final FinalDiffuseOptimization<ContextType> diffuseOptimization;

    public SpecularFit(ContextType context, IBRResources<ContextType> resources, SpecularFitSettings settings) throws FileNotFoundException
    {
        SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(resources, settings);

        // Textures calculated on CPU and passed to GPU (not framebuffers): basis functions & weights
        basisResources = new BasisResources<>(context, settings);

        // Normal optimization module that manages its own resources
        normalOptimization = new NormalOptimization<>(
            context, programFactory,
            estimationProgram ->
            {
                Drawable<ContextType> drawable = resources.createDrawable(estimationProgram);
                programFactory.setupShaderProgram(estimationProgram);
                basisResources.useWithShaderProgram(estimationProgram);
                return drawable;
            },
            settings);

        // Specular roughness / reflectivity module that manages its own resources
        roughnessOptimization = new RoughnessOptimization<>(context, basisResources, settings);

        // Final diffuse estimation
        diffuseOptimization = new FinalDiffuseOptimization<>(context, resources, settings);
    }

    @Override
    public void close()
    {
        basisResources.close();
        normalOptimization.close();
        roughnessOptimization.close();
        diffuseOptimization.close();
    }

    @Override
    public Texture2D<ContextType> getDiffuseMap()
    {
        return diffuseOptimization.getDiffuseMap();
    }

    @Override
    public Texture2D<ContextType> getNormalMap()
    {
        return normalOptimization.getNormalMap();
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
    public Texture3D<ContextType> getWeightMaps()
    {
        return basisResources.weightMaps;
    }

    @Override
    public Texture2D<ContextType> getBasisMaps()
    {
        return basisResources.basisMaps;
    }
}
