package tetzlaff.gl.util;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.*;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import de.javagl.jgltf.obj.model.ObjGltfModelCreator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        // Write glb file from converted model
        GltfModelWriter gltfModelWriter = new GltfModelWriter();
        Path glbPath = basePath.resolve("output/out.glb");
        gltfModelWriter.writeBinary(newModel, glbPath.toFile());
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
