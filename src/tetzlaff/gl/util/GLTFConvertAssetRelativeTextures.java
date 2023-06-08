package tetzlaff.gl.util;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetsV2;
import de.javagl.jgltf.obj.model.ObjGltfModelCreator;
import tetzlaff.ibrelight.export.specularfit.SpecularFitGltfExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GLTFConvertAssetRelativeTextures {

    static String OBJ_FILENAME = "kuan-yu.obj";

    public static void main(String[] args) throws IOException {

        // Convert the obj
        Path basePath = Paths.get("src/tetzlaff/gl/util");
        Files.createDirectories(basePath.resolve("output"));

        Path objPath = basePath.resolve(OBJ_FILENAME);
        ObjGltfModelCreator gltfModelCreator = new ObjGltfModelCreator();
        GltfModel gltfModel = gltfModelCreator.create(objPath.toUri());

        System.out.println("GltfModel extracted from OBJ");

        // Convert the resulting GltfModel to a GltfAsset and GlTF for lower-level access
        GltfAssetV2 gltfAsset = GltfAssetsV2.createEmbedded(gltfModel);

        SpecularFitGltfExporter exporter = new SpecularFitGltfExporter(gltfAsset);
        exporter.setDefaultNames();
        exporter.addWeightImages(10);

        exporter.write(basePath.resolve("output/out.glb").toFile());

    }

}
