package tetzlaff.ibrelight.export.specularfit;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.creation.MeshPrimitiveBuilder;
import de.javagl.jgltf.model.impl.DefaultMeshModel;
import de.javagl.jgltf.model.impl.DefaultMeshPrimitiveModel;
import de.javagl.jgltf.model.impl.DefaultNodeModel;
import de.javagl.jgltf.model.impl.DefaultSceneModel;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import de.javagl.jgltf.model.io.v2.GltfAssetsV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import tetzlaff.gl.util.VertexGeometry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

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

    public void setSpecularName(String uri)
    {
        TextureInfo texture = createRelativeTexture(uri, "specular");
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
        setSpecularName("specular.png");
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

    public static SpecularFitGltfExporter fromVertexGeometry(VertexGeometry geometry) throws IOException
    {
        GltfModelBuilder builder = GltfModelBuilder.create();

        DefaultSceneModel scene = new DefaultSceneModel();
        DefaultNodeModel node = new DefaultNodeModel();
        DefaultMeshModel mesh = new DefaultMeshModel();
        MeshPrimitiveBuilder primitiveBuilder = MeshPrimitiveBuilder.create();

        // Add positions, normals and texcords by buffer
        primitiveBuilder.addPositions3D(geometry.getVertices().getBuffer().asFloatBuffer());

        if (geometry.hasNormals())
        {
            primitiveBuilder.addNormals3D(geometry.getNormals().getBuffer().asFloatBuffer());
        }

        if (geometry.hasTexCoords())
        {
            // Copy to a new buffer while flipping vertically
            FloatBuffer inBuffer = geometry.getTexCoords().getBuffer().asFloatBuffer();
            FloatBuffer outBuffer = FloatBuffer.allocate(inBuffer.capacity());

            for (int i = 1; i < inBuffer.capacity(); i++) {
                float texCoord = inBuffer.get(i);

                // Flip Y coordinates, leave X
                if (i % 2 != 0)
                {
                    texCoord = 1.0f - texCoord;
                }

                outBuffer.put(i, texCoord);
            }

            primitiveBuilder.addTexCoords02D(outBuffer);
        }

        // Build the primitive
        DefaultMeshPrimitiveModel primitive = primitiveBuilder.build();

        // Create the material for the mesh
        MaterialModelV2 material = new MaterialModelV2();
        primitive.setMaterialModel(material);

        // Add the primitive to the mesh, to the node, to the scene
        mesh.addMeshPrimitiveModel(primitive);
        node.addMeshModel(mesh);
        scene.addNode(node);
        builder.addSceneModel(scene);

        // Build model and convert to embedded asset
        GltfAssetV2 gltfAsset = GltfAssetsV2.createEmbedded(builder.build());

        return new SpecularFitGltfExporter(gltfAsset);
    }
}
