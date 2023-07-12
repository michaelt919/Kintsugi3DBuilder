package tetzlaff.ibrelight.export.specularfit.gltf;

import de.javagl.jgltf.impl.v2.TextureInfo;

import java.util.ArrayList;
import java.util.List;

public class GltfMaterialSpecularWeights
{

    private int stride = 4;

    private List<TextureInfo> textures = new ArrayList<>();

    public List<TextureInfo> getTextures()
    {
        return textures;
    }

    public void addTexture(TextureInfo texture)
    {
        textures.add(texture);
    }

    public int getStride()
    {
        return stride;
    }

    public void setStride(int stride)
    {
        this.stride = stride;
    }

}
