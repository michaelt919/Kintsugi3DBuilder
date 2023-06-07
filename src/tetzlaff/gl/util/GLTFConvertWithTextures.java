package tetzlaff.gl.util;

import de.javagl.jgltf.impl.v2.Image;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.*;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import de.javagl.jgltf.model.io.v2.GltfAssetsV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import de.javagl.jgltf.obj.model.ObjGltfModelCreator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Runnable test to open a .obj file, convert it to glTF and add textures
 */
public class GLTFConvertWithTextures {

    static String OBJ_FILENAME = "kuan-yu.obj";

    public static void main(String[] args) throws IOException {
        // Convert the obj
        Path basePath = Paths.get("src/tetzlaff/gl/util");
        Files.createDirectories(basePath.resolve("output"));

        Path objPath = basePath.resolve(OBJ_FILENAME);
        ObjGltfModelCreator gltfModelCreator = new ObjGltfModelCreator();
        GltfModel gltfModel = gltfModelCreator.create(objPath.toUri());

        System.out.println("GltfModel extracted from OBJ");

        // Create a new GltfModelBuilder
        GltfModelBuilder builder = GltfModelBuilder.create();
        DefaultSceneModel scene = new DefaultSceneModel();

        MaterialModelV2 material = new MaterialModelV2();

        // Load the diffuse texture to glTF model
        DefaultTextureModel diffuseTexture = loadTextureFromDisk(Paths.get("src/tetzlaff/gl/util/diffuse.png"));
        DefaultTextureModel normalTexture = loadTextureFromDisk(Paths.get("src/tetzlaff/gl/util/normal.png"));
        DefaultTextureModel roughnessTexture = loadTextureFromDisk(Paths.get("src/tetzlaff/gl/util/roughness.png"));

        // For some reason, normal maps are not added from the material to the glTFModel during serialization,
        // so it needs to be added manually here.
        builder.addTextureModel(normalTexture);

        material.setBaseColorTexture(diffuseTexture);
        material.setNormalTexture(normalTexture);
        material.setMetallicRoughnessTexture(roughnessTexture);

        // Can arbitrary textures be added to the glTF? Yes! It's not part of the material, but it is included in the output file
        // However is this a good idea? Doing this would make the client load a single huge file instead of several smaller files.
        // Same goes for textures. Would it be better to force the embedded uri to images be a relative url to png hosted on the server?
        // Maybe this could let the client choose a quality setting by appending '-lowres' to filenames or something and hosting
        // 'modelname-albedo-lowres.png', 'modelname-albedo-medres.png', etc on the server.
        DefaultTextureModel weights00Texture = loadTextureFromDisk(Paths.get("src/tetzlaff/gl/util/weights00.png"));
        weights00Texture.setName("weights00");
        builder.addTextureModel(weights00Texture);

        // So can we add a texture that is just a uri?
        DefaultTextureModel urlTextureModel = new DefaultTextureModel();
        DefaultImageModel urlImageModel = new DefaultImageModel();

        // Trick the MIME-type detector into writing "image/url"
        urlImageModel.setUri(".url");

        // Something in the Gltf writer really wants the imageData field to not be null, throwing a NullPointerException without it
        // Yet specifying an image data buffer overwrites the uri set above... de.javagl.jgltf.model.io.v2.EmbeddedAssetCreatorV2.convertImageToEmbedded

        // Write the url into the image data field
        ByteBuffer emptyByteBuffer = ByteBuffer.wrap("https://example.com/example-texture.png".getBytes());
        urlImageModel.setImageData(emptyByteBuffer);

        urlTextureModel.setImageModel(urlImageModel);

        builder.addTextureModel(urlTextureModel);

        for (MeshModel mesh : gltfModel.getMeshModels()) {

            DefaultMeshModel meshModel = new DefaultMeshModel();
            for (MeshPrimitiveModel meshPrimitive : mesh.getMeshPrimitiveModels()) {

                DefaultMeshPrimitiveModel primModel = (DefaultMeshPrimitiveModel) meshPrimitive;
                primModel.setMaterialModel(material);
                meshModel.addMeshPrimitiveModel(primModel);

            }

            DefaultNodeModel node = new DefaultNodeModel();
            node.addMeshModel(meshModel);
            scene.addNode(node);
        }

        builder.addSceneModel(scene);

        GltfModel newModel = builder.build();
        System.out.println("Built converted glTF");

        GltfAssetV2 gltfAsset = GltfAssetsV2.createEmbedded(newModel);

        // For images that we've hacked external image urls onto from above, replace the uri field with the actual url
        // instead of the base64 encoded url.
        // Are we sure external URLs are supposed to be supported by glTF? Because they certainly aren't supported by jgltf.
        for (Image img : gltfAsset.getGltf().getImages()) {
            String url = img.getUri();
            if (url.startsWith("data:image/url")) {
                String urlb64 = url.replace("data:image/url;base64,", "");
                String decodedUrl = new String(Base64.getDecoder().decode(urlb64));
                img.setUri(decodedUrl);
            }
        }

        // Write glb file from converted model
        //GltfModelWriter gltfModelWriter = new GltfModelWriter();
        //gltfModelWriter.writeEmbedded(newModel, glbPath.toFile());
        Path glbPath = basePath.resolve("output/out.glb");
        GltfAssetWriterV2 gltfAssetWriter = new GltfAssetWriterV2();
        gltfAssetWriter.writeBinary(gltfAsset, new FileOutputStream(glbPath.toFile()));
    }

    private static DefaultTextureModel loadTextureFromDisk(Path path) throws IOException {

        DefaultTextureModel textureModel = new DefaultTextureModel();
        DefaultImageModel imageModel = new DefaultImageModel();

        byte[] normalData = Files.readAllBytes(path);
        ByteBuffer normalImageData = ByteBuffer.wrap(normalData);

        imageModel.setImageData(normalImageData);
        textureModel.setImageModel(imageModel);

        return textureModel;

    }

}
