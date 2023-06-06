package tetzlaff.gl.util;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.DefaultMeshModel;
import de.javagl.jgltf.model.impl.DefaultNodeModel;
import de.javagl.jgltf.model.impl.DefaultSceneModel;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.obj.model.ObjGltfModelCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Runnable test to open a .obj file, convert it to glTF and add textures
 */
public class GLTFConvertWithTextures {

    static String OBJ_FILENAME = "ding.obj";

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

        for (MeshModel mesh : gltfModel.getMeshModels()) {

            DefaultMeshModel meshModel = new DefaultMeshModel();
            for (MeshPrimitiveModel meshPrimitive : mesh.getMeshPrimitiveModels()) {

                meshModel.addMeshPrimitiveModel(meshPrimitive);

            }

            DefaultNodeModel node = new DefaultNodeModel();
            node.addMeshModel(meshModel);
            scene.addNode(node);
        }

        builder.addSceneModel(scene);

        GltfModel newModel = builder.build();
        System.out.println("Built converted glTF");


        GltfModelWriter gltfModelWriter = new GltfModelWriter();
        Path glbPath = basePath.resolve("output/out.glb");
        gltfModelWriter.writeBinary(newModel, glbPath.toFile());
    }

}
