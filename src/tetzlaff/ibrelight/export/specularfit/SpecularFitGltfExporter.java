package tetzlaff.ibrelight.export.specularfit;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import tetzlaff.gl.util.VertexGeometry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SpecularFitGltfExporter {

    private GltfAssetV2 asset;

    public SpecularFitGltfExporter(GltfAssetV2 glTfAsset)
    {
        this.asset = glTfAsset;
    }

    public void setDiffuseName(String uri)
    {
        TextureInfo texture = createRelativeTexture(uri, "diffuse");
        asset.getGltf().getMaterials().forEach((material -> {
            material.getPbrMetallicRoughness().setBaseColorTexture(texture);
        }));
    }

    public void setNormalName(String uri)
    {
        TextureInfo texInfo = createRelativeTexture(uri, "normal");
        MaterialNormalTextureInfo matNormInfo = convertTexInfoToNormal(texInfo);
        asset.getGltf().getMaterials().forEach((material -> {
            material.setNormalTexture(matNormInfo);
        }));
    }

    public void setRoughnessName(String uri)
    {
        TextureInfo texture = createRelativeTexture(uri, "roughness");
        asset.getGltf().getMaterials().forEach((material -> {
            material.getPbrMetallicRoughness().setMetallicRoughnessTexture(texture);
        }));
    }

    public void addWeightImages(int basisCount)
    {
        for (int b = 0; b < basisCount; b++)
        {
            String weightsFilename = SpecularFitSerializer.getWeightFileName(b);
            String weightName = weightsFilename.split("\\.")[0];
            createRelativeTexture(weightsFilename, weightName);
        }
    }

    public void write(File file) throws IOException
    {
        GltfAssetWriterV2 writer = new GltfAssetWriterV2();
        writer.writeBinary(asset, new FileOutputStream(file));
    }

    public void tryWrite(File file)
    {
        try {
            write(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultNames()
    {
        setDiffuseName("diffuse.png");
        setNormalName("normal.png");
        setRoughnessName("roughness.png");
    }

    private static MaterialNormalTextureInfo convertTexInfoToNormal(TextureInfo normalTextureInfo, float scale)
    {
        MaterialNormalTextureInfo normInfo = new MaterialNormalTextureInfo();

        normInfo.setIndex(normalTextureInfo.getIndex());
        normInfo.setTexCoord(normalTextureInfo.getTexCoord());
        normInfo.setExtensions(normalTextureInfo.getExtensions());
        normInfo.setExtras(normalTextureInfo.getExtras());
        normInfo.setScale(scale);

        return normInfo;
    }

    private static MaterialNormalTextureInfo convertTexInfoToNormal(TextureInfo normalTextureInfo)
    {
        return convertTexInfoToNormal(normalTextureInfo, 1.0f);
    }

    private TextureInfo createRelativeTexture(String uri, String name)
    {
        GlTF gltf = asset.getGltf();

        Image image = new Image();
        image.setUri(uri);
        gltf.addImages(image);
        int imageIndex = gltf.getImages().size() - 1;

        Texture texture = new Texture();
        texture.setSource(imageIndex);
        texture.setName(name);
        gltf.addTextures(texture);
        int textureIndex = gltf.getTextures().size() - 1;

        TextureInfo info = new TextureInfo();
        info.setIndex(textureIndex);

        return info;
    }

    private TextureInfo createRelativeTexture(String uri)
    {
        return createRelativeTexture(uri, null);
    }

    public static SpecularFitGltfExporter fromVertexGeometry(VertexGeometry geometry)
    {
        return null; //TODO: Load vertex geometry into a GltfAsset and instantiate SpecularFitGltfExporter from there
    }
}
