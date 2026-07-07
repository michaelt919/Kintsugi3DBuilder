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

package kintsugi3d.builder.io.gltf.kintsugi3dviewer;

import de.javagl.jgltf.impl.v2.TextureInfo;
import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.io.gltf.CustomTextureExport;
import kintsugi3d.builder.io.gltf.PBRExporter;
import kintsugi3d.builder.io.gltf.StandardTextureExport;
import kintsugi3d.builder.resources.project.specular.TextureResources;

import java.io.File;

/**
 * Exports just what is needed for Kintsugi 3D Viewer
 */
public class Kintsugi3DViewerExporter extends PBRExporter
{
    private final Kintsugi3DViewerExtras extras = new Kintsugi3DViewerExtras();

    @StandardTextureExport(StandardTexture.DIFFUSE_COLOR)
    public void diffuseColor(TextureInfo diffuseColor)
    {
        extras.setDiffuseTexture(diffuseColor);
    }

    @CustomTextureExport("constant")
    public void diffuseConstant(TextureInfo diffuseConstant)
    {
        extras.setDiffuseConstantTexture(diffuseConstant);
    }

    @StandardTextureExport(StandardTexture.SPECULAR_COLOR)
    public void specularColor(TextureInfo specularColor)
    {
        extras.setSpecularTexture(specularColor);
    }

    @StandardTextureExport(StandardTexture.ROUGHNESS)
    public void roughness(TextureInfo roughness)
    {
        // Does nothing in the glTF but flags single-channel roughness to be saved alongside the other textures.
    }

    @StandardTextureExport(StandardTexture.ERROR)
    public void error(TextureInfo error)
    {
        // Does nothing but flags that the error texture needs to be exported
    }

    @Override
    public void finishTextures()
    {
        if (getTextureResources().getBasisResources() != null)
        {
            int basisCount = getTextureResources().getBasisResources().getBasisCount();
            if (basisCount > 0)
            {
                // Add weight images
                SpecularWeights weights = new SpecularWeights();

                if (shouldCombineWeights())
                {
                    weights.setStride(TextureResources.WEIGHTS_PER_PACKED_CHANNEL);
                    for (int b = 0; b * weights.getStride() < basisCount; b++)
                    {
                        String weightmapName = TextureResources.getPackedWeightMapName(b);
                        TextureInfo weightTexInfo = addTexture(weightmapName, true);
                        weights.addTexture(weightTexInfo);
                    }
                }
                else
                {
                    weights.setStride(1);
                    for (int b = 0; b < basisCount; b++)
                    {
                        String weightmapName = TextureResources.getUnpackedWeightMapName(b);
                        TextureInfo weightTexInfo = addTexture(weightmapName, true);
                        weights.addTexture(weightTexInfo);
                    }
                }

                extras.setSpecularWeights(weights);
            }
        }
    }

    @Override
    public void saveTextures(File outputDirectory)
    {
        super.saveTextures(outputDirectory);

        // Check if we need to fallback to PNG to ensure alpha channels exist.
        String format = determineFileFormat(getTextureFileFormat(), true);

        if (shouldCombineWeights())
        {
            getTextureResources().savePackedWeightMaps(format, outputDirectory, getTextureFilePrefix());
        }
        else
        {
            getTextureResources().saveUnpackedWeightMaps(format, outputDirectory, getTextureFilePrefix());
        }

        getTextureResources().saveBasisFunctions(outputDirectory,
            TextureResources.getBasisFunctionsFilename(getTextureFilePrefix()));
    }

    @Override
    public Kintsugi3DViewerExtras getExtras()
    {
        return extras;
    }
}
