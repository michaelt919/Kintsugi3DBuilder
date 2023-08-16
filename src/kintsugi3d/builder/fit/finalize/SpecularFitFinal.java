/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit.finalize;

import java.io.File;
import java.io.IOException;

import kintsugi3d.builder.fit.FilenameConstants;
import kintsugi3d.builder.fit.SpecularFitBase;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.builder.core.TextureFitSettings;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;

/**
 * Can do the roughness / ORM map fit, hole fill, etc., but should not need access to the original photographs
 * @param <ContextType>
 */
public final class SpecularFitFinal<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private final Texture2D<ContextType> diffuseMap;
    private final Texture2D<ContextType> normalMap;
    private final Texture2D<ContextType> constantMap;
    private final Texture2D<ContextType> quadraticMap;


    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> createEmpty(
        ContextType context, TextureFitSettings textureFitSettings,
        SpecularBasisSettings specularBasisSettings, boolean includeConstant) throws IOException
    {
        return new SpecularFitFinal<>(context, textureFitSettings, specularBasisSettings, includeConstant);
    }

    private SpecularFitFinal(ContextType context, TextureFitSettings textureFitSettings,
        SpecularBasisSettings specularBasisSettings, boolean includeConstant) throws IOException
    {
        super(context, textureFitSettings, specularBasisSettings);

        // Allocate diffuse map
        diffuseMap = context.getTextureFactory()
            .build2DColorTexture(textureFitSettings.width, textureFitSettings.height)
            .setInternalFormat(ColorFormat.RGB8)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();

        // Allocate normal map
        normalMap = context.getTextureFactory()
            .build2DColorTexture(textureFitSettings.width, textureFitSettings.height)
            .setInternalFormat(ColorFormat.RGB8)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();

        // Allocate constant map
        constantMap = includeConstant ?
            context.getTextureFactory()
                .build2DColorTexture(textureFitSettings.width, textureFitSettings.height)
                .setInternalFormat(ColorFormat.RGB8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture()
            : null;

        // Allocate constant map
        quadraticMap = includeConstant ?
            context.getTextureFactory()
                .build2DColorTexture(textureFitSettings.width, textureFitSettings.height)
                .setInternalFormat(ColorFormat.RGB8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture()
            : null;
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

        // Load diffuse map
        diffuseMap = context.getTextureFactory()
            .build2DColorTextureFromFile(new File(priorSolutionDirectory, FilenameConstants.getDiffuseFilename("PNG")), true)
            .setLinearFilteringEnabled(true)
            .createTexture();

        // Load normal map
        normalMap = context.getTextureFactory()
            .build2DColorTextureFromFile(new File(priorSolutionDirectory, FilenameConstants.getNormalFilename("PNG")), true)
            .setLinearFilteringEnabled(true)
            .createTexture();

        // Load constant map
        File constantMapFile = new File(priorSolutionDirectory, FilenameConstants.getConstantFilename("PNG"));
        constantMap = constantMapFile.exists() ?
            context.getTextureFactory()
                .build2DColorTextureFromFile(constantMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

        // Load constant map
        File quadraticMapFile = new File(priorSolutionDirectory, FilenameConstants.getQuadraticFilename("PNG"));
        quadraticMap = quadraticMapFile.exists() ?
            context.getTextureFactory()
                .build2DColorTextureFromFile(quadraticMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

        getBasisResources().loadFromPriorSolution(priorSolutionDirectory);
        getBasisWeightResources().loadFromPriorSolution(priorSolutionDirectory);
    }

    @Override
    public void close()
    {
        super.close();

        if (diffuseMap != null)
        {
            diffuseMap.close();
        }

        if (normalMap != null)
        {
            normalMap.close();
        }

        if (constantMap != null)
        {
            constantMap.close();
        }

        if (quadraticMap != null)
        {
            quadraticMap.close();
        }
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

    @Override
    public Texture2D<ContextType> getConstantMap()
    {
        return constantMap;
    }

    @Override
    public Texture2D<ContextType> getQuadraticMap()
    {
        return quadraticMap;
    }
}
