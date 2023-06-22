/*
 *  Copyright (c) Michael Tetzlaff 2022
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
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

/**
 * A class that bundles all of the GPU resources for representing a final specular fit solution.
 * @param <ContextType>
 */
@SuppressWarnings("PackageVisibleField")
public class SpecularFitFromOptimization<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    /**
     * Final diffuse estimate
     */
    final FinalDiffuseOptimization<ContextType> diffuseOptimization;

    /**
     * Estimated surface normals
     */
    final NormalOptimization<ContextType> normalOptimization;

    public SpecularFitFromOptimization(IBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory,
        TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings, NormalOptimizationSettings normalOptimizationSettings)
        throws FileNotFoundException
    {
        super(resources.getContext(), textureFitSettings, specularBasisSettings);

        // Final diffuse estimation
        diffuseOptimization = new FinalDiffuseOptimization<>(programFactory, textureFitSettings);

        // Normal optimization module that manages its own resources
        normalOptimization = new NormalOptimization<ContextType>(
            programFactory,
            estimationProgram ->
            {
                Drawable<ContextType> drawable = programFactory.createDrawable(estimationProgram);
                programFactory.setupShaderProgram(estimationProgram);
                basisResources.useWithShaderProgram(estimationProgram);
                return drawable;
            },
            textureFitSettings, normalOptimizationSettings);
    }

    @Override
    public void close()
    {
        diffuseOptimization.close();
        normalOptimization.close();
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
}
