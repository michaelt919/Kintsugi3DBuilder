/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit.gltf;

import de.javagl.jgltf.impl.v2.TextureInfo;

public class GltfMaterialExtras
{

    private TextureInfo diffuseTexture = null;

    private TextureInfo specularTexture = null;

    private String basisFunctionsUri = null;

    private GltfMaterialSpecularWeights specularWeights = null;

    public TextureInfo getSpecularTexture()
    {
        return specularTexture;
    }

    public void setSpecularTexture(TextureInfo specularTexture)
    {
        this.specularTexture = specularTexture;
    }

    public String getBasisFunctionsUri()
    {
        return basisFunctionsUri;
    }

    public void setBasisFunctionsUri(String basisFunctionsUri)
    {
        this.basisFunctionsUri = basisFunctionsUri;
    }

    public GltfMaterialSpecularWeights getSpecularWeights()
    {
        return specularWeights;
    }

    public void setSpecularWeights(GltfMaterialSpecularWeights specularWeights)
    {
        this.specularWeights = specularWeights;
    }

    public TextureInfo getDiffuseTexture()
    {
        return diffuseTexture;
    }

    public void setDiffuseTexture(TextureInfo diffuseTexture)
    {
        this.diffuseTexture = diffuseTexture;
    }
}
