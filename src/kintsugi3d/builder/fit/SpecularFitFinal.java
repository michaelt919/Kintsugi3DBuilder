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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.finalize.AlbedoORMOptimization;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;

/**
 * Can do the roughness / ORM map fit, hole fill, etc., but should not need access to the original photographs
 * @param <ContextType>
 */
public final class SpecularFitFinal<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private final Texture2D<ContextType> diffuseMap;
    private final Texture2D<ContextType> normalMap;
    private final Texture2D<ContextType> constantMap;
//    private final Texture2D<ContextType> quadraticMap;
    private final AlbedoORMOptimization<ContextType> albedoORMOptimization;

    private final Map<String, Texture2D<ContextType>> metadataMaps;

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> createEmpty(
        SpecularMaterialResources<ContextType> original, TextureResolution textureResolution,
        Collection<String> metadataMapNames, SpecularBasisSettings specularBasisSettings, boolean includeConstant) throws IOException
    {
        return new SpecularFitFinal<>(original, textureResolution, metadataMapNames, specularBasisSettings, includeConstant);
    }

    private SpecularFitFinal(SpecularMaterialResources<ContextType> original, TextureResolution textureResolution,
        Collection<String> metadataMapNames, SpecularBasisSettings specularBasisSettings, boolean includeConstant) throws IOException
    {
        super(original.getContext(), textureResolution, specularBasisSettings);

        ContextType context = original.getContext();

        // Allocate diffuse map
        diffuseMap = context.getTextureFactory()
            .build2DColorTexture(textureResolution.width, textureResolution.height)
            .setInternalFormat(ColorFormat.RGB8)
            .setLinearFilteringEnabled(true)
            .createTexture();

        // Allocate normal map
        normalMap = context.getTextureFactory()
            .build2DColorTexture(textureResolution.width, textureResolution.height)
            .setInternalFormat(ColorFormat.RGB8)
            .setLinearFilteringEnabled(true)
            .createTexture();

        // Allocate constant map
        constantMap = includeConstant ?
            context.getTextureFactory()
                .build2DColorTexture(textureResolution.width, textureResolution.height)
                .setInternalFormat(ColorFormat.RGB8)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

//        // Allocate quadratic map
//        quadraticMap = includeConstant ?
//            context.getTextureFactory()
//                .build2DColorTexture(textureResolution.width, textureResolution.height)
//                .setInternalFormat(ColorFormat.RGB8)
//                .setLinearFilteringEnabled(true)
//                .setMipmapsEnabled(true)
//                .createTexture()
//            : null;

        // Allocate metadata maps
        metadataMaps = new HashMap<>(metadataMapNames.size());
        for (String key : metadataMapNames)
        {
            metadataMaps.put(key,
                context.getTextureFactory()
                    .build2DColorTexture(textureResolution.width, textureResolution.height)
                    .setInternalFormat(ColorFormat.RGBA8)
                    .setLinearFilteringEnabled(true)
                    .createTexture());
        }

        albedoORMOptimization = original.getOcclusionMap() == null ?
            AlbedoORMOptimization.createWithoutOcclusion(context, textureResolution) :
            AlbedoORMOptimization.createWithOcclusion(original.getOcclusionMap(), textureResolution);
    }

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory) throws IOException
    {
        return new SpecularFitFinal<>(context, SpecularFitOptimizable.getSerializableMetadataMapNames(), priorSolutionDirectory);
    }

    private SpecularFitFinal(ContextType context, Collection<String> metadataMapNames, File priorSolutionDirectory) throws IOException
    {
        super(context, priorSolutionDirectory);

        // Load diffuse map
        File diffuseMapFile = new File(priorSolutionDirectory, "diffuse.png");
        diffuseMap = diffuseMapFile.exists() ?
            context.getTextureFactory()
                .build2DColorTextureFromFile(diffuseMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

        // Load normal map
        File normalMapFile = new File(priorSolutionDirectory, "normal.png");
        normalMap = normalMapFile.exists() ?
            context.getTextureFactory()
                .build2DColorTextureFromFile(normalMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

        // Load constant map
        File constantMapFile = new File(priorSolutionDirectory, "constant.png");
        constantMap = constantMapFile.exists() ?
            context.getTextureFactory()
                .build2DColorTextureFromFile(constantMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

//        // Load quadratic map
//        File quadraticMapFile = new File(priorSolutionDirectory, "quadratic.png");
//        quadraticMap = quadraticMapFile.exists() ?
//            context.getTextureFactory()
//                .build2DColorTextureFromFile(quadraticMapFile, true)
//                .setLinearFilteringEnabled(true)
//                .createTexture()
//            : null;

        // Load metadata maps
        metadataMaps = new HashMap<>(metadataMapNames.size());
        for (String key : metadataMapNames)
        {
            File metadataMapFile = new File(priorSolutionDirectory, key + ".png");
            if (metadataMapFile.exists())
            {
                metadataMaps.put(key,
                    context.getTextureFactory()
                        .build2DColorTextureFromFile(metadataMapFile, true)
                        .setLinearFilteringEnabled(true)
                        .createTexture());
            }
        }

        AlbedoORMOptimization<ContextType> albedoORMOptimizationTemp = null;
        try
        {
            albedoORMOptimizationTemp = AlbedoORMOptimization.loadFromPriorSolution(context, priorSolutionDirectory);

            if (albedoORMOptimizationTemp.getAlbedoMap() == null)
            {
                // Load failed
                albedoORMOptimizationTemp.close();

                // Try to initialize based on diffuse map resolution
                if (diffuseMap != null)
                {
                    albedoORMOptimizationTemp = AlbedoORMOptimization.createWithoutOcclusion(context,
                        new TextureResolution(diffuseMap.getWidth(), diffuseMap.getHeight()));
                }
            }
        }
        catch (IOException e)
        {
            // Load failed; try to initialize based on diffuse map resolution
            if (diffuseMap != null)
            {
                albedoORMOptimizationTemp = AlbedoORMOptimization.createWithoutOcclusion(context,
                    new TextureResolution(diffuseMap.getWidth(), diffuseMap.getHeight()));
            }
        }

        albedoORMOptimization = albedoORMOptimizationTemp;
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

//        if (quadraticMap != null)
//        {
//            quadraticMap.close();
//        }

        if (albedoORMOptimization != null)
        {
            albedoORMOptimization.close();
        }

        for (Texture2D<ContextType> metadataMap : metadataMaps.values())
        {
            metadataMap.close();
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

//    @Override
//    public Texture2D<ContextType> getQuadraticMap()
//    {
//        return quadraticMap;
//    }

    @Override
    public Texture2D<ContextType> getAlbedoMap()
    {
        return albedoORMOptimization == null ? null : albedoORMOptimization.getAlbedoMap();
    }
    @Override
    public Texture2D<ContextType> getORMMap()
    {
        return albedoORMOptimization == null ? null : albedoORMOptimization.getORMMap();
    }

    @Override
    public Map<String, Texture2D<ContextType>> getMetadataMaps()
    {
        return Collections.unmodifiableMap(metadataMaps);
    }

    public AlbedoORMOptimization<ContextType> getAlbedoORMOptimization()
    {
        return albedoORMOptimization;
    }
}
