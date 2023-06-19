package tetzlaff.ibrelight.export.specularfit;

import de.javagl.jgltf.impl.v2.TextureInfo;

import java.util.ArrayList;
import java.util.List;

public class GltfMaterialExtras
{

    private TextureInfo roughnessTexture = null;

    private TextureInfo specularTexture = null;

    private String basisFunctionsUri = null;

    private GltfMaterialSpecularWeights specularWeights = null;

    public TextureInfo getRoughnessTexture()
    {
        return roughnessTexture;
    }

    public void setRoughnessTexture(TextureInfo roughnessTexture)
    {
        this.roughnessTexture = roughnessTexture;
    }

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

}
