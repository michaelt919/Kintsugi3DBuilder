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
