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
import java.util.ArrayList;

public class SpecularFitGltfExporter
{

    private GltfAssetV2 asset;

    private TextureInfo diffuseTexture, normalTexture, roughnessMetallicTexture;

    private TextureInfo specularTexture, roughnessTexture;

    private ArrayList<TextureInfo> weightTextures;

    GltfMaterialExtras extraData = new GltfMaterialExtras();

    public SpecularFitGltfExporter(GltfAssetV2 glTfAsset)
    {
        this.asset = glTfAsset;
    }

    public void setDiffuseUri(String uri)
    {
        if (diffuseTexture == null)
        {
            diffuseTexture = createRelativeTexture(uri, "diffuse");
            asset.getGltf().getMaterials().forEach((material -> {
                material.getPbrMetallicRoughness().setBaseColorTexture(diffuseTexture);
            }));
        }
        else
        {
            setTextureUri(diffuseTexture, uri);
        }
    }

    public void setNormalUri(String uri)
    {
        if (normalTexture == null) {
            normalTexture = createRelativeTexture(uri, "normal");
            MaterialNormalTextureInfo matNormInfo = convertTexInfoToNormal(normalTexture);
            asset.getGltf().getMaterials().forEach((material -> {
                material.setNormalTexture(matNormInfo);
            }));
        }
        else
        {
            setTextureUri(normalTexture, uri);
        }
    }

    public void setRoughnessMetallicUri(String uri)
    {
        if (roughnessMetallicTexture == null)
        {
            roughnessMetallicTexture = createRelativeTexture(uri, "roughnessMetallic");
            asset.getGltf().getMaterials().forEach((material -> {
                material.getPbrMetallicRoughness().setMetallicRoughnessTexture(roughnessMetallicTexture);
            }));
        }
        else
        {
            setTextureUri(roughnessMetallicTexture, uri);
        }
    }

    private void setRoughnessUri(String uri)
    {
        if (roughnessTexture == null)
        {
            roughnessTexture = createRelativeTexture(uri, "roughness");
            extraData.setRoughnessTexture(roughnessTexture);
        }
    }

    public void setSpecularUri(String uri)
    {
        if (specularTexture == null)
        {
            specularTexture = createRelativeTexture(uri, "specular");
            extraData.setSpecularTexture(specularTexture);
        }
        else
        {
            setTextureUri(specularTexture, uri);
        }
    }

    public void setBasisFunctionsUri(String uri)
    {
        extraData.setBasisFunctionsUri(uri);
    }

    public void addWeightImages(int basisCount)
    {
        GltfMaterialSpecularWeights weights = new GltfMaterialSpecularWeights();
        weights.setStride(1);
        for (int b = 0; b < basisCount; b++)
        {
            String weightsFilename = SpecularFitSerializer.getWeightFileName(b);
            String weightName = weightsFilename.split("\\.")[0];
            TextureInfo weightTexInfo = createRelativeTexture(weightsFilename, weightName);
            weights.addTexture(weightTexInfo);
        }
        extraData.setSpecularWeights(weights);
    }

    public void write(File file) throws IOException
    {
        asset.getGltf().getMaterials().forEach((material -> {
            material.setExtras(extraData);
        }));

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
        setDiffuseUri("diffuse.png");
        setNormalUri("normal.png");
        setRoughnessMetallicUri("orm.png");
        setSpecularUri("specular.png");
        setRoughnessUri("roughness.png");
        setBasisFunctionsUri("basisFunctions.csv");
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

    private void setTextureUri(TextureInfo textureInfo, String newUri)
    {
        GlTF gltf = asset.getGltf();

        if (gltf.getTextures().size() <= textureInfo.getIndex())
            return;

        Texture texture = gltf.getTextures().get(textureInfo.getIndex());

        if (gltf.getImages().size() <= texture.getSource())
            return;

        Image image = gltf.getImages().get(texture.getSource());
        image.setUri(newUri);
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
