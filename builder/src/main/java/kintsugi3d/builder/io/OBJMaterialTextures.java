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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.texture.StandardTexture;
import kintsugi3d.gl.material.ImportedMaterial;
import kintsugi3d.gl.material.ReadonlyMaterialTextureMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OBJMaterialTextures implements TextureSupplier
{
    private final ImportedMaterial material;
    private final File directory;

    public OBJMaterialTextures(ImportedMaterial material, File directory)
    {
        this.material = material;
        this.directory = directory;
    }

    @Override
    public Map<String, File> getTextures()
    {
        Map<String, File> textures = new HashMap<>(8);

        putTextureInMap(textures, StandardTexture.DIFFUSE_COLOR, material.getDiffuseMap());
        putTextureInMap(textures, StandardTexture.SPECULAR_COLOR, material.getSpecularMap());
        putTextureInMap(textures, StandardTexture.ROUGHNESS, material.getRoughnessMap());
        putTextureInMap(textures, StandardTexture.OCCLUSION, material.getAmbientOcclusionMap());

        if (material.getNormalMap() != null)
        {
            putTextureInMap(textures, StandardTexture.NORMAL_MAP, material.getNormalMap());
        }
        else
        {
            // Metashape (and possibly other programs) uses bump map as normal map.
            putTextureInMap(textures, StandardTexture.NORMAL_MAP, material.getBumpMap());
        }

        return textures;
    }

    private void putTextureInMap(Map<String, File> textures, StandardTexture textureType, ReadonlyMaterialTextureMap diffuseMap)
    {
        if (diffuseMap != null)
        {
            File textureFile = new File(directory, diffuseMap.getMapName());
            if (textureFile.exists())
            {
                textures.put(textureType.details.name, new File(directory, diffuseMap.getMapName()));
            }
        }
    }
}
