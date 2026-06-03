/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.finalize.AlbedoORMOptimization;
import kintsugi3d.builder.fit.settings.BasisSettings;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Can do the roughness / ORM map fit, hole fill, etc., but should not need access to the original photographs
 * @param <ContextType>
 */
public final class SpecularFitFinal<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private final Map<String, Texture2D<ContextType>> managedTextures;
    private final AlbedoORMOptimization<ContextType> albedoORMOptimization;

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> createEmpty(
        TextureResources<ContextType> original, TextureResolution textureResolution, BasisSettings basisSettings) throws IOException
    {
        return new SpecularFitFinal<>(original, textureResolution, basisSettings);
    }

    private SpecularFitFinal(TextureResources<ContextType> original, TextureResolution textureResolution, BasisSettings basisSettings)
        throws IOException
    {
        super(original.getContext(), textureResolution, basisSettings);

        ContextType context = original.getContext();

        managedTextures = new HashMap<>(original.getTextures().size());

        // Copy all textures not handled elsewhere
        original.getTextures().entrySet().stream()
            .filter(entry -> 
                // Skip specular color and roughness maps that are handled by SpecularFitBase:
                !StandardTexture.SPECULAR_COLOR.texName.equals(entry.getKey()) && !StandardTexture.ROUGHNESS.texName.equals(entry.getKey()))
            .collect(Collectors.toMap(Entry::getKey,
                entry -> context.getTextureFactory()
                    .build2DColorTexture(textureResolution.width, textureResolution.height)
                    .setInternalFormat(entry.getValue().getInternalUncompressedColorFormat()) // copy format of the original.
                    .setLinearFilteringEnabled(true)
                    .createTexture()));

        Texture2D<ContextType> occlusionMap = original.getTexture(StandardTexture.OCCLUSION);
        albedoORMOptimization = occlusionMap == null ?
            AlbedoORMOptimization.createWithoutOcclusion(context, textureResolution) :
            AlbedoORMOptimization.createWithOcclusion(occlusionMap, textureResolution);
    }

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory) throws IOException
    {
        return new SpecularFitFinal<>(context, priorSolutionDirectory);
    }

    private SpecularFitFinal(ContextType context, File priorSolutionDirectory) throws IOException
    {
        super(context, priorSolutionDirectory);
        
        managedTextures = new HashMap<>(StandardTexture.values().length);

        addStandardTexture(StandardTexture.DIFFUSE_COLOR, priorSolutionDirectory);
        addStandardTexture(StandardTexture.NORMAL_MAP, priorSolutionDirectory);
        addStandardTexture(StandardTexture.ERROR, priorSolutionDirectory);

        // TODO store a list of non-standard textures in the project file rather than hard-coding here.
        addTexture("constant", priorSolutionDirectory);

        AlbedoORMOptimization<ContextType> albedoORMOptimizationTemp = null;
        try
        {
            albedoORMOptimizationTemp = AlbedoORMOptimization.loadFromPriorSolution(context, priorSolutionDirectory);

            if (albedoORMOptimizationTemp.getAlbedoMap() == null)
            {
                // Load failed
                albedoORMOptimizationTemp.close();

                AlbedoORMOptimization<ContextType> fallback = albedoDiffuseFallback();
                albedoORMOptimizationTemp = fallback != null ? fallback : albedoORMOptimizationTemp;
            }
        }
        catch (IOException e)
        {
            AlbedoORMOptimization<ContextType> fallback = albedoDiffuseFallback();
            albedoORMOptimizationTemp = fallback != null ? fallback : albedoORMOptimizationTemp;
        }

        albedoORMOptimization = albedoORMOptimizationTemp;
    }

    private AlbedoORMOptimization<ContextType> albedoDiffuseFallback() throws IOException
    {
        // Load failed; try to initialize based on diffuse map resolution
        if (managedTextures.containsKey(StandardTexture.DIFFUSE_COLOR.texName))
        {
            Texture2D<ContextType> diffuseMap = managedTextures.get(StandardTexture.DIFFUSE_COLOR.texName);
            if (diffuseMap != null)
            {
                return AlbedoORMOptimization.createWithoutOcclusion(getContext(),
                    new TextureResolution(diffuseMap.getWidth(), diffuseMap.getHeight()));
            }
        }

        return null;
    }

    private void addStandardTexture(StandardTexture standardTex, File priorSolutionDirectory) throws IOException
    {
        // Load texture file
        Texture2D<ContextType> texture = loadTexture(standardTex.texName, priorSolutionDirectory);

        if (texture != null)
        {
            managedTextures.put(standardTex.texName, texture);
        }
    }

    private void addTexture(String texName, File priorSolutionDirectory) throws IOException
    {
        // Load texture file
        Texture2D<ContextType> texture = loadTexture(texName, priorSolutionDirectory);

        if (texture != null)
        {
            managedTextures.put(texName, texture);
        }
    }

    @Override
    public Map<String, Texture2D<ContextType>> getTextures()
    {
        Map<String, Texture2D<ContextType>> mergedMaps =
            new HashMap<>(getSpecularTextureCount() + managedTextures.size() + albedoORMOptimization.getTextureCount());
        mergedMaps.putAll(getSpecularTextures());
        mergedMaps.putAll(managedTextures);
        mergedMaps.putAll(albedoORMOptimization.getTextures());
        return Collections.unmodifiableMap(mergedMaps);
    }

    public AlbedoORMOptimization<ContextType> getAlbedoORMOptimization()
    {
        return albedoORMOptimization;
    }

    @Override
    public void close()
    {
        super.close();

        for (Texture2D<ContextType> texture : managedTextures.values())
        {
            texture.close();
        }
        managedTextures.clear();

        if (albedoORMOptimization != null)
        {
            albedoORMOptimization.close();
        }
    }
}
