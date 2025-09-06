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

package kintsugi3d.builder.io.gltf;

import de.javagl.jgltf.impl.v2.TextureInfo;

public class GLTFMaterialExtras
{

    private TextureInfo diffuseTexture = null;

    private TextureInfo diffuseConstantTexture = null;

    private TextureInfo specularTexture = null;

    private String basisFunctionsUri = null;

    private GLTFMaterialSpecularWeights specularWeights = null;

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

    public GLTFMaterialSpecularWeights getSpecularWeights()
    {
        return specularWeights;
    }

    public void setSpecularWeights(GLTFMaterialSpecularWeights specularWeights)
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

    public TextureInfo getDiffuseConstantTexture()
    {
        return diffuseConstantTexture;
    }

    public void setDiffuseConstantTexture(TextureInfo diffuseConstantTexture)
    {
        this.diffuseConstantTexture = diffuseConstantTexture;
    }
}
