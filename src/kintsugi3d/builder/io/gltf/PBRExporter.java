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

package kintsugi3d.builder.io.gltf;

import de.javagl.jgltf.impl.v2.MaterialNormalTextureInfo;
import de.javagl.jgltf.impl.v2.MaterialOcclusionTextureInfo;
import de.javagl.jgltf.impl.v2.TextureInfo;
import kintsugi3d.builder.core.StandardTexture;

/**
 * Base class for glTF material export that ensures a PBR fallback that should look presentable for most models in typical viewers
 */
public class PBRExporter extends MaterialExporter
{
    @StandardTextureExport(StandardTexture.ALBEDO)
    public void baseColor(TextureInfo baseColor)
    {
        getAsset().getGltf().getMaterials().forEach(
            material -> material.getPbrMetallicRoughness().setBaseColorTexture(baseColor));
    }

    @StandardTextureExport(StandardTexture.ORM)
    public void orm(TextureInfo orm)
    {
        getAsset().getGltf().getMaterials().forEach(material ->
        {
            material.getPbrMetallicRoughness().setMetallicRoughnessTexture(orm);

            MaterialOcclusionTextureInfo occlusion = convertTexInfoToOcclusion(orm);
            material.setOcclusionTexture(occlusion);
        });
    }

    @StandardTextureExport(StandardTexture.NORMAL_MAP)
    public void normalMap(MaterialNormalTextureInfo normal)
    {
        getAsset().getGltf().getMaterials().forEach(material -> material.setNormalTexture(normal));
    }
}
