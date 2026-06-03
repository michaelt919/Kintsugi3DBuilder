/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.gltf;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.model.MaterialModel;
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
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

public class ModelExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(ModelExporter.class);

    private final GltfAssetV2 asset;

    private MaterialExporter materialExporter;
    private int modelNodeIndex = 0;

    public ModelExporter(GltfAssetV2 glTfAsset, int modelNodeIndex)
    {
        this.asset = glTfAsset;
        this.modelNodeIndex = modelNodeIndex;
    }

    public GltfAssetV2 getAsset()
    {
        return asset;
    }

    public MaterialExporter getMaterialExporter()
    {
        return materialExporter;
    }

    public void setMaterialExporter(MaterialExporter materialExporter)
    {
        this.materialExporter = materialExporter;
        materialExporter.setAsset(asset);
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

    public void exportWithTextures(File modelFile, Runnable finishedCallback) throws IOException
    {
        exportModelOnly(modelFile);

        if (materialExporter.getTextureResources() != null)
        {
            Rendering.runLater(() ->
            {
                exportTextures(modelFile.getParentFile());

                if (finishedCallback != null)
                {
                    finishedCallback.run();
                }
            });
        }
        else if (finishedCallback != null) // not saving textures
        {
            finishedCallback.run();
        }
    }

    public void exportModelOnly(File file) throws IOException
    {
        // Apply materials to model before exporting.
        materialExporter.apply();

        GltfAssetWriterV2 writer = new GltfAssetWriterV2();

        try (FileOutputStream out = new FileOutputStream(file))
        {
            writer.writeBinary(asset, out);
        }
    }

    private void exportTextures(File outputDirectory)
    {
        materialExporter.saveTextures(outputDirectory);

        LODGenerator lodGenerator = LODGenerator.getInstance();
        materialExporter.makeLODs(outputDirectory);

        materialExporter.postExport();
    }

    public static ModelExporter fromVertexGeometry(ReadonlyVertexGeometry geometry) throws IOException
    {
        return fromVertexGeometry(geometry, Matrix4.IDENTITY);
    }

    public static ModelExporter fromVertexGeometry(ReadonlyVertexGeometry geometry, Matrix4 transformation) throws IOException
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

        Matrix3 linearTranspose = transformation.getUpperLeft3x3().transpose();
        Matrix3 inverseTranspose = linearTranspose.inverse();

        if (geometry.hasNormals())
        {
            // Copy to a new buffer while applying transformation
            FloatBuffer normInBuffer = geometry.getNormals().getBuffer().asFloatBuffer();
            FloatBuffer normOutBuffer = FloatBuffer.allocate(normInBuffer.capacity());

            for (int i = 0; i < normInBuffer.capacity(); i += 3)
            {
                Vector3 normal;

                if (linearTranspose.determinant() == 0.0f)
                {
                    // Avoid indeterminate edge cases resulting in NaN and breaking
                    normal = Vector3.ZERO;
                }
                else
                {
                    // Ignore translation, only rotation and scale, but normalized to cancel scale
                    // Inverse transpose to support non-linear scale
                    normal = inverseTranspose.times(new Vector3(normInBuffer.get(i), normInBuffer.get(i+1), normInBuffer.get(i+2))).normalizedSafe();
                }

                normOutBuffer.put(i, normal.x);
                normOutBuffer.put(i+1, normal.y);
                normOutBuffer.put(i+2, normal.z);
            }

            primitiveBuilder.addNormals3D(normOutBuffer);

            if (geometry.hasTexCoords()) // has tangents if both normals and tex coords
            {
                // Copy to a new buffer while applying transformation
                FloatBuffer tangInBuffer = geometry.getTangents().getBuffer().asFloatBuffer();
                FloatBuffer tangOutBuffer = FloatBuffer.allocate(tangInBuffer.capacity());

                for (int i = 0; i < tangInBuffer.capacity(); i += 4)
                {
                    Vector3 tangent;

                    if (linearTranspose.determinant() == 0.0f)
                    {
                        // Avoid indeterminate edge cases resulting in NaN and breaking glTF which disallows NaN
                        tangent = Vector3.ZERO;
                    }
                    else
                    {
                        // Ignore translation, only rotation and scale, but normalized to cancel scale
                        // Inverse transpose to support non-linear scale
                        tangent = inverseTranspose.times(new Vector3(tangInBuffer.get(i), tangInBuffer.get(i + 1), tangInBuffer.get(i + 2))).normalizedSafe();
                    }

                    tangOutBuffer.put(i, tangent.x);
                    tangOutBuffer.put(i + 1, tangent.y);
                    tangOutBuffer.put(i + 2, tangent.z);
                    tangOutBuffer.put(i + 3, tangInBuffer.get(i + 3)); // bitangent sign
                }

                primitiveBuilder.addTangents4D(tangOutBuffer);
            }
        }

        if (geometry.hasTexCoords())
        {
            // Copy to a new buffer while flipping vertically
            FloatBuffer inBuffer = geometry.getTexCoords().getBuffer().asFloatBuffer();
            FloatBuffer outBuffer = FloatBuffer.allocate(inBuffer.capacity());

            for (int i = 0; i < inBuffer.capacity(); i++)
            {
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
        MaterialModel material = new MaterialModelV2();
        primitive.setMaterialModel(material);

        // Add the primitive to the mesh, to the node, to the scene
        mesh.addMeshPrimitiveModel(primitive);
        node.addMeshModel(mesh);
        scene.addNode(node);
        builder.addSceneModel(scene);

        // Build model and convert to embedded asset
        GltfAssetV2 gltfAsset = GltfAssetsV2.createEmbedded(builder.build());

        return new ModelExporter(gltfAsset, scene.getNodeModels().size() - 1);
    }
}
