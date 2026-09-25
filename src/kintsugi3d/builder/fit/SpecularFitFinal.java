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
import kintsugi3d.builder.fit.decomposition.BasisMaterialInfo;
import kintsugi3d.builder.fit.decomposition.MutableBasisResources;
import kintsugi3d.builder.fit.decomposition.MutableMaterialBasis;
import kintsugi3d.builder.fit.finalize.AlbedoORMOptimization;
import kintsugi3d.builder.fit.finalize.FinalDiffuseOptimization;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResources;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.util.MappedChange;
import kintsugi3d.builder.util.MappedChange.Type;
import kintsugi3d.builder.util.Observable;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.ReadonlyTexture2D;
import kintsugi3d.gl.core.Texture2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Can do the roughness / ORM map fit, hole fill, etc., but should not need access to the original photographs
 *
 * @param <ContextType>
 */
public final class SpecularFitFinal<ContextType extends Context<ContextType>>
    extends SpecularFitBase<ContextType> implements TextureResources<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitFinal.class);

    private final Map<TextureInfo, Texture2D<ContextType>> managedTextures;
    private final AlbedoORMOptimization<ContextType> albedoORMOptimization;

    private final MutableBasisResources<ContextType> mutableBasisResources;

    private Observable<MappedChange<String, BasisMaterialInfo>> basisObservable;

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> createEmpty(
        ReadonlyTextureResources<ContextType> original, MutableMaterialBasis basis, TextureResolution textureResolution) throws IOException
    {
        return new SpecularFitFinal<>(original, basis, textureResolution);
    }

    private SpecularFitFinal(ReadonlyTextureResources<ContextType> original,
                             MutableBasisResources<ContextType> basisResources, TextureResolution textureResolution)
        throws IOException
    {
        super(basisResources.getContext(), basisResources, basisResources.getWeightResources(), textureResolution);
        this.mutableBasisResources = basisResources;

        ContextType context = original.getContext();

        managedTextures = new HashMap<>(original.getTextures().size());

        // Copy all textures not handled elsewhere
        managedTextures.putAll(original.getTextures().entrySet().stream()
            .filter(entry ->
                // Skip specular color and roughness maps that are handled by SpecularFitBase:
                !StandardTexture.SPECULAR_COLOR.details.equals(entry.getKey()) && !StandardTexture.ROUGHNESS.details.equals(entry.getKey()))
            .collect(Collectors.toMap(Entry::getKey,
                entry -> context.getTextureFactory()
                    .build2DColorTexture(textureResolution.width, textureResolution.height)
                    .setInternalFormat(entry.getValue().getInternalUncompressedColorFormat()) // copy format of the original.
                    .setLinearFilteringEnabled(true)
                    .createTexture())));

        ReadonlyTexture2D<ContextType> occlusionMap = original.getTexture(StandardTexture.OCCLUSION);
        albedoORMOptimization = occlusionMap == null ?
            AlbedoORMOptimization.createWithoutOcclusion(context, textureResolution) :
            AlbedoORMOptimization.createWithOcclusion(occlusionMap.copy(), textureResolution);
    }

    private SpecularFitFinal(ReadonlyTextureResources<ContextType> original, MutableMaterialBasis basis, TextureResolution textureResolution)
        throws IOException
    {
        this(original, new MutableBasisResources<>(original.getContext(), basis, textureResolution), textureResolution);
    }

    private SpecularFitFinal(
        ContextType context, MutableBasisResources<ContextType> basisResources, File priorSolutionDirectory)
        throws IOException
    {
        super(context, basisResources, basisResources != null ? basisResources.getWeightResources() : null, priorSolutionDirectory);

        mutableBasisResources = basisResources;

        managedTextures = new HashMap<>(StandardTexture.values().length);

        addStandardTexture(StandardTexture.DIFFUSE_COLOR, priorSolutionDirectory);
        addStandardTexture(StandardTexture.NORMAL_MAP, priorSolutionDirectory);
        addStandardTexture(StandardTexture.ERROR, priorSolutionDirectory);

        // TODO store a list of non-standard textures in the project file rather than hard-coding here.
        addTexture(FinalDiffuseOptimization.CONSTANT_TRANSLUCENCY_MAP, priorSolutionDirectory);

        AlbedoORMOptimization<ContextType> albedoORMOptimizationTemp = null;
        try
        {
            albedoORMOptimizationTemp = AlbedoORMOptimization.loadFromPriorSolution(context, priorSolutionDirectory);
        }
        catch (IOException e)
        {
            LOG.error("Error loading albedo / ORM maps", e);
        }

        albedoORMOptimization = albedoORMOptimizationTemp;
    }

    private SpecularFitFinal(ContextType context, File priorSolutionDirectory) throws IOException
    {
        this(context, MutableBasisResources.loadFromPriorSolution(context, priorSolutionDirectory), priorSolutionDirectory);
    }

    public static <ContextType extends Context<ContextType>> SpecularFitFinal<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory) throws IOException
    {
        return new SpecularFitFinal<>(context, priorSolutionDirectory);
    }

    private void addStandardTexture(StandardTexture standardTex, File priorSolutionDirectory) throws IOException
    {
        // Load texture file
        Texture2D<ContextType> texture = loadTexture(standardTex.details.name, priorSolutionDirectory);

        if (texture != null)
        {
            managedTextures.put(standardTex.details, texture);
        }
    }

    private void addTexture(TextureInfo textureInfo, File priorSolutionDirectory) throws IOException
    {
        // Load texture file
        Texture2D<ContextType> texture = loadTexture(textureInfo.name, priorSolutionDirectory);

        if (texture != null)
        {
            managedTextures.put(textureInfo, texture);
        }
    }

    @Override
    public Map<TextureInfo, Texture2D<ContextType>> getTextures()
    {
        Map<TextureInfo, Texture2D<ContextType>> mergedMaps =
            new HashMap<>(getSpecularTextureCount() + managedTextures.size() + albedoORMOptimization.getTextureCount());
        mergedMaps.putAll(getSpecularTextures());
        mergedMaps.putAll(managedTextures);
        mergedMaps.putAll(albedoORMOptimization.getTextures());
        return Collections.unmodifiableMap(mergedMaps);
    }

    @Override
    public void toggleBasisMaterial(String materialName)
    {
        BasisMaterialInfo toggled = mutableBasisResources.toggleBasisMaterial(materialName);

        if (basisObservable != null)
        {
            basisObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, materialName, toggled));
        }
    }

    @Override
    public void deleteBasisMaterial(String materialName)
    {
        BasisMaterialInfo removed = mutableBasisResources.deleteBasisMaterial(materialName);

        if (basisObservable != null)
        {
            basisObservable.notifyObservers(new MappedChange<>(Type.REMOVED, materialName, removed));
        }
    }

    @Override
    public void setBasisObservable(Observable<MappedChange<String, BasisMaterialInfo>> basisObservable)
    {
        this.basisObservable = basisObservable;
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
