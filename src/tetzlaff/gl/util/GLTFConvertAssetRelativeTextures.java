package tetzlaff.gl.util;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import de.javagl.jgltf.model.io.v2.GltfAssetsV2;
import de.javagl.jgltf.obj.model.ObjGltfModelCreator;

import java.io.FileOutputStream;
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
        GlTF gltf = gltfAsset.getGltf();

        // Create Textures
        TextureInfo diffuseTexInfo = createRelativeTexture(gltf, "diffuse.png");
        TextureInfo roughnessTexture = createRelativeTexture(gltf, "roughness.png");
        TextureInfo normalTextureInfo = createRelativeTexture(gltf, "normal.png");
        MaterialNormalTextureInfo matNormInfo = convertTexInfoToNormal(normalTextureInfo, 1.0f);

        // Cycle through any materials in the asset, and set the textures for each
        for (Material material : gltf.getMaterials()) {
            MaterialPbrMetallicRoughness pbr = new MaterialPbrMetallicRoughness();
            pbr.setBaseColorTexture(diffuseTexInfo);
            pbr.setMetallicRoughnessTexture(roughnessTexture); // Technically wrong, need to remap this texture and generate metalic map
            material.setPbrMetallicRoughness(pbr);
            material.setNormalTexture(matNormInfo);
        }

        // Add an arbitrary weights texture
        TextureInfo weight00Tex = createRelativeTexture(gltf, "weights00.png", "weights00");

        Path glbPath = basePath.resolve("output/out.glb");
        GltfAssetWriterV2 gltfAssetWriter = new GltfAssetWriterV2();
        gltfAssetWriter.writeBinary(gltfAsset, new FileOutputStream(glbPath.toFile()));

    }

    private static MaterialNormalTextureInfo convertTexInfoToNormal(TextureInfo normalTextureInfo, float scale) {
        MaterialNormalTextureInfo normInfo = new MaterialNormalTextureInfo();

        normInfo.setIndex(normalTextureInfo.getIndex());
        normInfo.setTexCoord(normalTextureInfo.getTexCoord());
        normInfo.setExtensions(normalTextureInfo.getExtensions());
        normInfo.setExtras(normalTextureInfo.getExtras());
        normInfo.setScale(scale);

        return normInfo;
    }

    private static TextureInfo createRelativeTexture(GlTF gltf, String uri, String name) {
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

    private static TextureInfo createRelativeTexture(GlTF gltf, String uri) {
        return createRelativeTexture(gltf, uri, null);
    }

}
