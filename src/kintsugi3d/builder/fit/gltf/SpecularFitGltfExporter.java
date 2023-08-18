/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit.gltf;

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
import kintsugi3d.builder.export.specular.SpecularFitSerializer;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

public class SpecularFitGltfExporter
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitGltfExporter.class);

    private final GltfAssetV2 asset;

    private int modelNodeIndex = 0;

    private TextureInfo baseColorTexture, normalTexture, roughnessMetallicTexture;

    private TextureInfo diffuseTexture, diffuseConstantTexture, specularTexture;

    GltfMaterialExtras extraData = new GltfMaterialExtras();

    public SpecularFitGltfExporter(GltfAssetV2 glTfAsset, int modelNodeIndex)
    {
        this.asset = glTfAsset;
        this.modelNodeIndex = modelNodeIndex;
    }

    public void setBaseColorUri(String uri)
    {
        if (baseColorTexture == null)
        {
            baseColorTexture = createRelativeTexture(uri, "baseColor");
            asset.getGltf().getMaterials().forEach((material -> material.getPbrMetallicRoughness().setBaseColorTexture(baseColorTexture)));
        }
        else
        {
            setTextureUri(baseColorTexture, uri);
        }
    }

    public void addBaseColorLods(String baseUri, int baseRes, int minRes)
    {
        if (baseColorTexture == null)
            return;

        addLodsToTexture(baseColorTexture, baseUri, baseRes, minRes);
    }

    private void addLodsToTexture(TextureInfo textureInfo, String baseUri, int baseRes, int minRes)
    {
        Texture tex = getTexForInfo(textureInfo);
        GlTF gltf = asset.getGltf();

        GltfTextureExtras extras = new GltfTextureExtras();

        extras.setBaseRes(baseRes);

        String filename = baseUri;
        String extension = "";
        int i = filename.lastIndexOf('.'); //Strip file extension
        if (i > 0)
        {
            extension = filename.substring(i);
            filename = filename.substring(0, i);
        }

        // size = 2048, 1024, 512... minRes
        for (int size = baseRes / 2; size >= minRes; size /= 2)
        {
            Image image = new Image();
            image.setUri(filename + "-" + size + extension);
            gltf.addImages(image);
            int imageIndex = gltf.getImages().size() - 1;
            extras.setLodImageIndex(size, imageIndex);
        }

        tex.setExtras(extras);
    }

    public void setDiffuseUri(String uri)
    {
        if (diffuseTexture == null)
        {
            diffuseTexture = createRelativeTexture(uri, "diffuse");
            extraData.setDiffuseTexture(diffuseTexture);
        }
        else
        {
            setTextureUri(baseColorTexture, uri);
        }
    }

    public void addDiffuseLods(String baseUri, int baseRes, int minRes)
    {
        if (diffuseTexture == null)
            return;

        addLodsToTexture(diffuseTexture, baseUri, baseRes, minRes);
    }

    public void setDiffuseConstantUri(String uri)
    {
        if (diffuseConstantTexture == null)
        {
            diffuseConstantTexture = createRelativeTexture(uri, "diffuseConstant");
            extraData.setDiffuseConstantTexture(diffuseConstantTexture);
        }
        else
        {
            setTextureUri(diffuseConstantTexture, uri);
        }
    }

    public void addDiffuseConstantLods(String baseUri, int baseRes, int minRes)
    {
        if (diffuseConstantTexture == null)
            return;

        addLodsToTexture(diffuseConstantTexture, baseUri, baseRes, minRes);
    }

    public void setNormalUri(String uri)
    {
        if (normalTexture == null) {
            normalTexture = createRelativeTexture(uri, "normal");
            MaterialNormalTextureInfo matNormInfo = convertTexInfoToNormal(normalTexture);
            asset.getGltf().getMaterials().forEach((material -> material.setNormalTexture(matNormInfo)));
        }
        else
        {
            setTextureUri(normalTexture, uri);
        }
    }

    public void addNormalLods(String baseUri, int baseRes, int minRes)
    {
        if (normalTexture == null)
            return;

        addLodsToTexture(normalTexture, baseUri, baseRes, minRes);
    }

    public void setRoughnessMetallicUri(String uri)
    {
        if (roughnessMetallicTexture == null)
        {
            roughnessMetallicTexture = createRelativeTexture(uri, "roughnessMetallic");
            asset.getGltf().getMaterials().forEach((material -> material.getPbrMetallicRoughness().setMetallicRoughnessTexture(roughnessMetallicTexture)));
        }
        else
        {
            setTextureUri(roughnessMetallicTexture, uri);
        }
    }

    public void addRoughnessMetallicLods(String baseUri, int baseRes, int minRes)
    {
        if (roughnessMetallicTexture == null)
            return;

        addLodsToTexture(roughnessMetallicTexture, baseUri, baseRes, minRes);
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

    public void addSpecularLods(String baseUri, int baseRes, int minRes)
    {
        if (specularTexture == null)
            return;

        addLodsToTexture(specularTexture, baseUri, baseRes, minRes);
    }

    public void setBasisFunctionsUri(String uri)
    {
        extraData.setBasisFunctionsUri(uri);
    }

    public void addWeightImages(int basisCount)
    {
        addWeightImages(basisCount, false);
    }

    public void addWeightImages(int basisCount, boolean combined)
    {
        GltfMaterialSpecularWeights weights = new GltfMaterialSpecularWeights();
        weights.setStride(combined ? 4 : 1);
        for (int b = 0; b < basisCount / weights.getStride(); b++)
        {
            String weightsFilename = combined ? SpecularFitSerializer.getCombinedWeightFilename(b) : SpecularFitSerializer.getWeightFileName(b);
            String weightName = weightsFilename.split("\\.")[0];
            TextureInfo weightTexInfo = createRelativeTexture(weightsFilename, weightName);
            weights.addTexture(weightTexInfo);
        }
        extraData.setSpecularWeights(weights);
    }

    public void addWeightImageLods(int basisCount, int baseRes, int minRes)
    {
        if (extraData.getSpecularWeights() == null)
            return;

        boolean combined = extraData.getSpecularWeights().getStride() == 4;

        for (int b = 0; b < basisCount / extraData.getSpecularWeights().getStride(); b++)
        {
            String weightsFilename = combined ? SpecularFitSerializer.getCombinedWeightFilename(b) : SpecularFitSerializer.getWeightFileName(b);
            addLodsToTexture(extraData.getSpecularWeights().getTextures().get(b), weightsFilename, baseRes, minRes);
        }
    }

    public void setTranslation(Vector3 translation)
    {
        GlTF gltf = asset.getGltf();
        gltf.getNodes().get(modelNodeIndex).setTranslation(new float[]{translation.x, translation.y, translation.z});
    }

    public void setRotation(Vector4 quaternion)
    {
        GlTF gltf = asset.getGltf();
        gltf.getNodes().get(modelNodeIndex).setRotation(new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w});
    }

    public void setScale(Vector3 scale)
    {
        GlTF gltf = asset.getGltf();
        gltf.getNodes().get(modelNodeIndex).setScale(new float[]{scale.x, scale.y, scale.z});
    }

    public void setTransform(Matrix4 transform)
    {
        GlTF gltf = asset.getGltf();
        gltf.getNodes().get(modelNodeIndex).setMatrix(new float[]{
                transform.get(0,0), transform.get(1,0), transform.get(2,0), transform.get(3,0),
                transform.get(0,1), transform.get(1,1), transform.get(2,1), transform.get(3,1),
                transform.get(0,2), transform.get(1,2), transform.get(2,2), transform.get(3,2),
                transform.get(0,3), transform.get(1,3), transform.get(2,3), transform.get(3,3),
        });
    }

    public void write(File file) throws IOException
    {
        asset.getGltf().getMaterials().forEach((material -> material.setExtras(extraData)));

        GltfAssetWriterV2 writer = new GltfAssetWriterV2();
        writer.writeBinary(asset, new FileOutputStream(file));
    }

    public void tryWrite(File file)
    {
        try {
            write(file);
        } catch (IOException e) {
            log.error("Error writing glTF to disk:", e);
        }
    }

    public void setDefaultNames()
    {
        setBaseColorUri("albedo.png");
        setDiffuseUri("diffuse.png");
        setNormalUri("normal.png");
        setRoughnessMetallicUri("orm.png");
        setSpecularUri("specular.png");
        setBasisFunctionsUri("basisFunctions.csv");
    }

    public void addAllDefaultLods(int baseRes, int minRes)
    {
        addBaseColorLods("albedo.png", baseRes, minRes);
        addDiffuseLods("diffuse.png", baseRes, minRes);
        addNormalLods("normal.png", baseRes, minRes);
        addRoughnessMetallicLods("orm.png", baseRes, minRes);
        addSpecularLods("specular.png", baseRes, minRes);
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

    private Texture getTexForInfo(TextureInfo info)
    {
        GlTF gltf = asset.getGltf();
        return gltf.getTextures().get(info.getIndex());
    }

    private TextureInfo createRelativeTexture(String uri)
    {
        return createRelativeTexture(uri, null);
    }

    public static SpecularFitGltfExporter fromVertexGeometry(ReadonlyVertexGeometry geometry) throws IOException
    {
        return fromVertexGeometry(geometry, Matrix4.IDENTITY);
    }

    public static SpecularFitGltfExporter fromVertexGeometry(ReadonlyVertexGeometry geometry, Matrix4 transformation) throws IOException
    {
        GltfModelBuilder builder = GltfModelBuilder.create();

        DefaultSceneModel scene = new DefaultSceneModel();
        DefaultNodeModel node = new DefaultNodeModel();
        DefaultMeshModel mesh = new DefaultMeshModel();
        MeshPrimitiveBuilder primitiveBuilder = MeshPrimitiveBuilder.create();

        // Add positions, normals and texcords by buffer

        // Copy to a new buffer while applying transformation
        FloatBuffer posInBuffer = geometry.getVertices().getBuffer().asFloatBuffer();
        FloatBuffer posOutBuffer = FloatBuffer.allocate(posInBuffer.capacity());
        for (int i = 0; i < posInBuffer.capacity(); i+=3)
        {
            Vector4 position = new Vector4(posInBuffer.get(i), posInBuffer.get(i+1), posInBuffer.get(i+2), 1.0f);

            position = transformation.times(position);

            posOutBuffer.put(i, position.x);
            posOutBuffer.put(i+1, position.y);
            posOutBuffer.put(i+2, position.z);
        }
        primitiveBuilder.addPositions3D(posOutBuffer);

        if (geometry.hasNormals())
        {
            // Copy to a new buffer while applying transformation
            FloatBuffer normInBuffer = geometry.getNormals().getBuffer().asFloatBuffer();
            FloatBuffer normOutBuffer = FloatBuffer.allocate(normInBuffer.capacity());

            for (int i = 0; i < normInBuffer.capacity(); i += 3)
            {
                Vector3 normal = new Vector3(normInBuffer.get(i), normInBuffer.get(i+1), normInBuffer.get(i+2));

                // Ignore translation, only rotation and scale, but normalized to cancel uniform scale
                // Non-uniform scale is unsupported
                normal = transformation.getUpperLeft3x3().times(normal).normalized();

                normOutBuffer.put(i, normal.x);
                normOutBuffer.put(i+1, normal.y);
                normOutBuffer.put(i+2, normal.z);
            }

            primitiveBuilder.addNormals3D(normOutBuffer);
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

        return new SpecularFitGltfExporter(gltfAsset, scene.getNodeModels().size() - 1);
    }
}
