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

package kintsugi3d.gl.material;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.gl.core.*;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

// TODO Use more information from the material.  Currently just pulling texture names.
// TODO use glTF instead of / in addition to OBJ material?
public class ImportedMaterialResources<ContextType extends Context<ContextType>> implements ContextBound<ContextType>, Resource
{
    private static final Logger LOG = LoggerFactory.getLogger(ImportedMaterialResources.class);

    private final ContextType context;

    private final Map<StandardTexture, Texture2D<ContextType>> textures = new EnumMap<>(StandardTexture.class);

    @Override
    public ContextType getContext()
    {
        return context;
    }

    public Map<StandardTexture, Texture2D<ContextType>> getTextures()
    {
        return Collections.unmodifiableMap(textures);
    }

    public static <ContextType extends Context<ContextType>> ImportedMaterialResources<ContextType> createNull()
    {
        return new ImportedMaterialResources<>();
    }

    ImportedMaterialResources()
    {
        context = null;
    }

    ImportedMaterialResources(ContextType context, ReadonlyImportedMaterial material, File textureDirectory, TextureLoadOptions loadOptions) throws IOException
    {
        this.context = context;

        ImageFinder finder = ImageFinder.getInstance();

        try
        {
            File diffuseFile = finder.findImageFile(new File(textureDirectory, material.getDiffuseMap().getMapName()));
            LOG.info("Diffuse texture found.");
            Texture2D<ContextType> diffuseTex = context.getTextureFactory().build2DColorTextureFromFile(diffuseFile, true)
                .setInternalFormat(ColorFormat.RGB8)
                .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .createTexture();

            // diffuse map from photogrammetry can also be either as a diffuse color map or an albedo map
            textures.put(StandardTexture.DIFFUSE_COLOR, diffuseTex);
            textures.put(StandardTexture.ALBEDO, diffuseTex);
        }
        catch (FileNotFoundException ignored)
        {
            LOG.info("No diffuse texture found.");
        }

        try
        {
            File normalFile = finder.findImageFile(new File(textureDirectory, material.getNormalMap().getMapName()));
            LOG.info("Normal texture found.");
            textures.put(StandardTexture.NORMAL_MAP,
                context.getTextureFactory().build2DColorTextureFromFile(normalFile, true)
                    .setInternalFormat(ColorFormat.RG8)
                    .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                    .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                    .createTexture());
        }
        catch (FileNotFoundException ignored)
        {
            LOG.info("No normal texture found.");
        }

        try
        {
            File specularFile = finder.findImageFile(new File(textureDirectory, material.getSpecularMap().getMapName()));
            LOG.info("Specular texture found.");
            textures.put(StandardTexture.SPECULAR_COLOR,
                context.getTextureFactory().build2DColorTextureFromFile(specularFile, true)
                    .setInternalFormat(ColorFormat.RGB8)
                    .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                    .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                    .createTexture());
        }
        catch(FileNotFoundException ignored)
        {
            LOG.info("No specular texture found.");
        }

        try
        {
            File roughnessFile = finder.findImageFile(new File(textureDirectory, material.getRoughnessMap().getMapName()));
            LOG.info("Roughness texture found.");
            textures.put(StandardTexture.ROUGHNESS,
                context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true)
                    .setInternalFormat(ColorFormat.R8)
                    .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                    .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                    .createTexture());
        }
        catch (FileNotFoundException ignored)
        {
            LOG.info("No roughness texture found.");
        }

        try
        {
            File occlusionFile = finder.findImageFile(new File(textureDirectory, material.getAmbientOcclusionMap().getMapName()));
            LOG.info("Occlusion texture found.");
            textures.put(StandardTexture.OCCLUSION,
                context.getTextureFactory().build2DColorTextureFromFile(occlusionFile, true)
                    .setInternalFormat(ColorFormat.R8)
                    .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                    .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                    .createTexture());
        }
        catch (FileNotFoundException ignored)
        {
            LOG.info("No occlusion texture found.");
        }
    }

    @Override
    public void close()
    {
        for (Texture2D<ContextType> tex : textures.values())
        {
            tex.close();
        }

        textures.clear();
    }
}
