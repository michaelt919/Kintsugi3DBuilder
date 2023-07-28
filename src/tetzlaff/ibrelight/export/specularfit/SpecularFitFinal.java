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

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture2D;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.export.specularfit.settings.SpecularBasisSettings;

/**
 * Can do the roughness / ORM map fit, hole fill, etc., but should not need access to the original photographs
 * @param <ContextType>
 */
public final class SpecularFitFinal<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    /**
     * Diffuse map from file
     */
    private final Texture2D<ContextType> diffuseMap;

    /**
     * Normal map from file
     */
    private final Texture2D<ContextType> normalMap;

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> createEmpty(
        ContextType context, TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings) throws IOException
    {
        return new SpecularFitFinal<>(context, textureFitSettings, specularBasisSettings);
    }

    private SpecularFitFinal(ContextType context, TextureFitSettings textureFitSettings,
        SpecularBasisSettings specularBasisSettings) throws IOException
    {
        super(context, textureFitSettings, specularBasisSettings);

        // Load normal map
        diffuseMap = context.getTextureFactory()
            .build2DColorTexture(textureFitSettings.width, textureFitSettings.height)
            .setInternalFormat(ColorFormat.RGB8)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();

        // Load normal map
        normalMap = context.getTextureFactory()
            .build2DColorTexture(textureFitSettings.width, textureFitSettings.height)
            .setInternalFormat(ColorFormat.RGB8)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();
    }

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> loadFromPriorSolution(
        ContextType context, TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings,
        File priorSolutionDirectory) throws IOException
    {
        return new SpecularFitFinal<>(context, textureFitSettings, specularBasisSettings, priorSolutionDirectory);
    }

    private SpecularFitFinal(ContextType context, TextureFitSettings textureFitSettings,
        SpecularBasisSettings specularBasisSettings, File priorSolutionDirectory) throws IOException
    {
        super(context, textureFitSettings, specularBasisSettings);

        // Load normal map
        diffuseMap = context.getTextureFactory()
            .build2DColorTextureFromFile(new File(priorSolutionDirectory, "diffuse.png"), true)
            .setLinearFilteringEnabled(true)
            .createTexture();

        // Load normal map
        normalMap = context.getTextureFactory()
            .build2DColorTextureFromFile(new File(priorSolutionDirectory, "normal.png"), true)
            .setLinearFilteringEnabled(true)
            .createTexture();

        getBasisResources().loadFromPriorSolution(priorSolutionDirectory);
        getBasisWeightResources().loadFromPriorSolution(priorSolutionDirectory);
    }

    @Override
    public void close()
    {
        diffuseMap.close();
        normalMap.close();
    }

    @Override
    public Texture2D<ContextType> getDiffuseMap()
    {
        return diffuseMap;
    }

    @Override
    public Texture2D<ContextType> getNormalMap()
    {
        return normalMap;
    }
}
