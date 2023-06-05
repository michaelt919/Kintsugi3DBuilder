/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;

/**
 * A data structure for representing a geometry mesh consisting of vertex positions, surface normals, and texture coordinates.
 * @author Michael Tetzlaff
 *
 */
public final class VertexGeometry
{
    private File filename;

    private boolean hasNormals;
    private boolean hasTexCoords;

    private NativeVectorBuffer vertices;
    private NativeVectorBuffer normals;
    private NativeVectorBuffer texCoords;
    private NativeVectorBuffer tangents;
    private Vector3 centroid;
    private Vector3 boundingBoxCenter;
    private Vector3 boundingBoxSize;
    private float boundingRadius;

    private String materialFileName;
    private Material material; // TODO support multiple materials

    private VertexGeometry(File filename)
    {
        this.filename = filename;
    }

    private static class NormalTexCoordPair
    {
        public final int normalIndex;
        public final int texCoordIndex;

        NormalTexCoordPair(int normalIndex, int texCoordIndex)
        {
            this.normalIndex = normalIndex;
            this.texCoordIndex = texCoordIndex;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof NormalTexCoordPair)
            {
                NormalTexCoordPair otherPair = (NormalTexCoordPair) obj;
                return this.normalIndex == otherPair.normalIndex && this.texCoordIndex == otherPair.texCoordIndex;
            }
            else
            {
                return false;
            }
        }

        @Override
        public int hashCode()
        {
            int result = normalIndex;
            result = 31 * result + texCoordIndex;
            return result;
        }
    }

    /**
     * Initializes the mesh from a file containing the mesh in Wavefront OBJ format.
     * @param file The file to load.
     * @throws FileNotFoundException Thrown if any File I/O errors occur.
     */
    public static VertexGeometry createFromOBJFile(File file) throws FileNotFoundException
    {
        VertexGeometry inst = new VertexGeometry(file);

        Date timestamp = new Date();

        // Assume initially that normals and texture coordinates are present
        inst.hasNormals = true;
        inst.hasTexCoords = true;

        // Initialize dynamic tables to store the data from the file
        List<Vector3> vertexList = new ArrayList<>(100000);
        List<Vector3> normalList = new ArrayList<>(100000);
        Map<NormalTexCoordPair, Vector3> tangentMap = new HashMap<>(100000);
        Map<NormalTexCoordPair, Vector3> bitangentMap = new HashMap<>(100000);
        List<Vector2> texCoordList = new ArrayList<>(100000);
        List<Integer> vertexIndexList = new ArrayList<>(100000);
        List<Integer> normalIndexList = new ArrayList<>(100000);
        List<Integer> texCoordIndexList = new ArrayList<>(100000);

        Vector3 sum = Vector3.ZERO;

        String materialName = null;

        try(Scanner scanner = new Scanner(file))
        {
            while(scanner.hasNext())
            {
                String id = scanner.next();
                switch (id)
                {
                    case "mtllib":
                        if (inst.materialFileName == null)
                        {
                            // Use first material filename found
                            inst.materialFileName = scanner.next();
                        }
                        break;
                    case "usemtl":
                        if (materialName == null)
                        {
                            // Use first material found
                            materialName = scanner.next();
                        }
                        break;
                    case "v":
                        // Vertex position
                        float x = scanner.nextFloat();
                        float y = scanner.nextFloat();
                        float z = scanner.nextFloat();

                        sum = sum.plus(new Vector3(x, y, z));

                        vertexList.add(new Vector3(x, y, z));
                        break;
                    case "vt":
                        // Texture coordinate
                        if (inst.hasTexCoords)
                        {
                            texCoordList.add(new Vector2(scanner.nextFloat(), scanner.nextFloat()));
                        }
                        break;
                    case "vn":
                        if (inst.hasNormals)
                        {
                            // Vertex normal
                            float nx = scanner.nextFloat();
                            float ny = scanner.nextFloat();
                            float nz = scanner.nextFloat();

                            // Normalize to unit length
                            normalList.add(new Vector3(nx, ny, nz).normalized());
                        }
                        break;
                    case "f":
                        for (int i = 0; i < 3; i++) // Only support triangles
                        {
                            String[] parts = scanner.next().split("\\/");

                            // Process vertex position
                            int vertexIndex = Integer.parseInt(parts[0]);
                            if (vertexIndex < 0)
                            {
                                // Relative index
                                vertexIndexList.add(vertexList.size() + vertexIndex);
                            }
                            else
                            {
                                // Absolute index
                                // 1-based -> 0-based indexing
                                vertexIndexList.add(vertexIndex - 1);
                            }

                            if (parts.length < 2 || parts[1].isEmpty())
                            {
                                // No texture coordinate
                                inst.hasTexCoords = false;
                            }
                            else if (inst.hasTexCoords)
                            {
                                // Process texture coordinate
                                int texCoordIndex = Integer.parseInt(parts[1]);
                                if (texCoordIndex < 0)
                                {
                                    // Relative index
                                    texCoordIndexList.add(texCoordIndexList.size() + texCoordIndex);
                                }
                                else
                                {
                                    // Absolute index
                                    // 1-based -> 0-based indexing
                                    texCoordIndexList.add(texCoordIndex - 1);
                                }
                            }

                            if (parts.length < 3 || parts[2].isEmpty())
                            {
                                // No vertex normal
                                inst.hasNormals = false;
                            }
                            else if (inst.hasNormals)
                            {
                                // Process vertex normal
                                int normalIndex = Integer.parseInt(parts[2]);
                                if (normalIndex < 0)
                                {
                                    // Relative index
                                    normalIndexList.add(normalIndexList.size() + normalIndex);
                                }
                                else
                                {
                                    // Absolute index
                                    // 1-based -> 0-based indexing
                                    normalIndexList.add(normalIndex - 1);
                                }
                            }
                        }

                        if (inst.hasTexCoords)
                        {
                            if (inst.hasNormals)
                            {
                                Vector3 position0 = vertexList.get(vertexIndexList.get(vertexIndexList.size() - 3));
                                Vector3 position1 = vertexList.get(vertexIndexList.get(vertexIndexList.size() - 2));
                                Vector3 position2 = vertexList.get(vertexIndexList.get(vertexIndexList.size() - 1));

                                Vector2 texCoords0 = texCoordList.get(texCoordIndexList.get(texCoordIndexList.size() - 3));
                                Vector2 texCoords1 = texCoordList.get(texCoordIndexList.get(texCoordIndexList.size() - 2));
                                Vector2 texCoords2 = texCoordList.get(texCoordIndexList.get(texCoordIndexList.size() - 1));

                                Vector3[] tangents = computeTangents(position0, position1, position2, texCoords0, texCoords1, texCoords2);

                                // TODO broken code - make it so that two vertices share tangents if they share normals AND texture coordinates

                                NormalTexCoordPair pair0 = new NormalTexCoordPair(
                                    normalIndexList.get(normalIndexList.size() - 3),
                                    texCoordIndexList.get(texCoordIndexList.size() - 3));

                                NormalTexCoordPair pair1 = new NormalTexCoordPair(
                                    normalIndexList.get(normalIndexList.size() - 2),
                                    texCoordIndexList.get(texCoordIndexList.size() - 2));

                                NormalTexCoordPair pair2 = new NormalTexCoordPair(
                                    normalIndexList.get(normalIndexList.size() - 1),
                                    texCoordIndexList.get(texCoordIndexList.size() - 1));

                                tangentMap.put(pair0, tangentMap.getOrDefault(pair0, Vector3.ZERO).plus(tangents[0]));
                                tangentMap.put(pair1, tangentMap.getOrDefault(pair1, Vector3.ZERO).plus(tangents[0]));
                                tangentMap.put(pair2, tangentMap.getOrDefault(pair2, Vector3.ZERO).plus(tangents[0]));

                                bitangentMap.put(pair0, bitangentMap.getOrDefault(pair0, Vector3.ZERO).plus(tangents[1]));
                                bitangentMap.put(pair1, bitangentMap.getOrDefault(pair1, Vector3.ZERO).plus(tangents[1]));
                                bitangentMap.put(pair2, bitangentMap.getOrDefault(pair2, Vector3.ZERO).plus(tangents[1]));
                            }
                            else
                            {
                                // TODO
                            }
                        }
                        break;
                    default:
                        break;
                }

                // Always advance to the next line.
                scanner.nextLine();
            }
        }

        inst.centroid = sum.dividedBy(vertexList.size());

        Map<NormalTexCoordPair, Vector4> orthoTangentsMap = new HashMap<>(100000);
        for (Entry<NormalTexCoordPair, Vector3> entry : tangentMap.entrySet())
        {
            orthoTangentsMap.put(entry.getKey(),
                orthogonalizeTangent(normalList.get(entry.getKey().normalIndex), entry.getValue(), bitangentMap.get(entry.getKey())));
        }

        float boundingBoxMinX = 0.0f;
        float boundingBoxMinY = 0.0f;
        float boundingBoxMinZ = 0.0f;
        float boundingBoxMaxX = 0.0f;
        float boundingBoxMaxY = 0.0f;
        float boundingBoxMaxZ = 0.0f;
        inst.boundingRadius = 0.0f;

        // Copy the data from the dynamic tables into a data structure that OpenGL can use.
        int vertexCount = vertexIndexList.size();
        inst.vertices = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 3, vertexCount);
        int index = 0;
        for (int k : vertexIndexList)
        {
            Vector3 vertex = vertexList.get(k);

            boundingBoxMinX = Math.min(boundingBoxMinX, vertex.x);
            boundingBoxMinY = Math.min(boundingBoxMinY, vertex.y);
            boundingBoxMinZ = Math.min(boundingBoxMinZ, vertex.z);

            boundingBoxMaxX = Math.max(boundingBoxMaxX, vertex.x);
            boundingBoxMaxY = Math.max(boundingBoxMaxY, vertex.y);
            boundingBoxMaxZ = Math.max(boundingBoxMaxZ, vertex.z);

            inst.boundingRadius = Math.max(inst.boundingRadius, vertex.minus(inst.centroid).length());

            inst.vertices.set(index, 0, vertex.x);
            inst.vertices.set(index, 1, vertex.y);
            inst.vertices.set(index, 2, vertex.z);

            index++;
        }

        inst.boundingBoxCenter = new Vector3((boundingBoxMinX + boundingBoxMaxX) / 2, (boundingBoxMinY + boundingBoxMaxY) / 2, (boundingBoxMinZ + boundingBoxMaxZ) / 2);
        inst.boundingBoxSize = new Vector3(boundingBoxMaxX - boundingBoxMinX, boundingBoxMaxY - boundingBoxMinY, boundingBoxMaxZ - boundingBoxMinZ);

        if (inst.hasNormals)
        {
            inst.normals = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 3, vertexCount);
            int i = 0;
            for (int k : normalIndexList)
            {
                inst.normals.set(i, 0, normalList.get(k).x);
                inst.normals.set(i, 1, normalList.get(k).y);
                inst.normals.set(i, 2, normalList.get(k).z);
                i++;
            }
        }

        if (inst.hasTexCoords)
        {
            inst.texCoords = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 2, vertexCount);
            int i = 0;
            for (int k : texCoordIndexList)
            {
                inst.texCoords.set(i, 0, texCoordList.get(k).x);
                inst.texCoords.set(i, 1, texCoordList.get(k).y);
                i++;
            }
        }

        if (inst.hasTexCoords && inst.hasNormals)
        {
            inst.tangents = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, vertexCount);
            for (int i = 0; i < normalIndexList.size(); i++)
            {
                inst.tangents.set(i, 0, orthoTangentsMap.get(new NormalTexCoordPair(normalIndexList.get(i), texCoordIndexList.get(i))).x);
                inst.tangents.set(i, 1, orthoTangentsMap.get(new NormalTexCoordPair(normalIndexList.get(i), texCoordIndexList.get(i))).y);
                inst.tangents.set(i, 2, orthoTangentsMap.get(new NormalTexCoordPair(normalIndexList.get(i), texCoordIndexList.get(i))).z);
                inst.tangents.set(i, 3, orthoTangentsMap.get(new NormalTexCoordPair(normalIndexList.get(i), texCoordIndexList.get(i))).w);
            }
        }

        if (inst.materialFileName != null)
        {
            try
            {
                Dictionary<String, Material> materialLibrary = Material.loadFromMTLFile(new File(file.getParentFile(), inst.materialFileName));
                inst.material = materialLibrary.get(materialName);
            }
            catch(IOException e)
            {
                e.printStackTrace();
                inst.material = null;
            }
        }
        else
        {
            inst.material = null;
        }

        System.out.println("Mesh loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        return inst;
    }

    private static Vector3[] computeTangents(
            Vector3 position0, Vector3 position1, Vector3 position2,
            Vector2 texCoords0, Vector2 texCoords1, Vector2 texCoords2)
    {
        Vector3[] tangents = new Vector3[2];

        float s1 = texCoords1.x - texCoords0.x;
        float s2 = texCoords2.x - texCoords0.x;
        float t1 = texCoords1.y - texCoords0.y;
        float t2 = texCoords2.y - texCoords0.y;

        float r = 1.0f / (s1 * t2 - s2 * t1);

        Vector3 q1 = position1.minus(position0);
        Vector3 q2 = position2.minus(position0);

        tangents[0] = q1.times(r * t2).plus(q2.times(r * -t1));
        tangents[1] = q1.times(r * -s2).plus(q2.times(r * s1));

        return tangents;
    }

    private static Vector4 orthogonalizeTangent(Vector3 normal, Vector3 tangent, Vector3 bitangent)
    {
        Vector3 orthoTangent;
        Vector3 orthoBitangent;

        // Normal vector is assumed to already be normalized
        orthoTangent = tangent.minus(normal.times(normal.dot(tangent))).normalized();
        orthoBitangent = bitangent.minus(normal.times(normal.dot(bitangent)).minus(orthoTangent.times(orthoTangent.dot(bitangent)))).normalized();

        return orthoTangent.asVector4(orthoBitangent.dot(normal.cross(orthoTangent)));
    }

    /**
     * Gets whether or not the mesh has surface normals.
     * @return true if the mesh has surface normals, false otherwise.
     */
    public boolean hasNormals()
    {
        return hasNormals;
    }

    /**
     * Gets whether or not the mesh has texture coordinates.
     * @return true if the mesh has texture coordinates, false otherwise.
     */
    public boolean hasTexCoords()
    {
        return hasTexCoords;
    }

    public <ContextType extends Context<ContextType>> GeometryResources<ContextType> createGraphicsResources(ContextType context)
    {
        return new GeometryResources<>(context, this);
    }

    /**
     * Gets a packed list containing the vertex positions of the mesh that can be used by a GL.
     * @return A packed list containing the vertex positions.
     */
    public NativeVectorBuffer getVertices()
    {
        return vertices;
    }

    /**
     * Gets a packed list containing the surface normals of the mesh that can be used by a GL.
     * @return A packed list containing the surface normals.
     */
    public NativeVectorBuffer getNormals()
    {
        return normals;
    }

    /**
     * Gets a packed list containing the texture coordinates of the mesh that can be used by a GL.
     * @return A packed list containing the texture coordinates.
     */
    public NativeVectorBuffer getTexCoords()
    {
        return texCoords;
    }

    /**
     * Gets the centroid of the mesh - that is, the average of all the vertex positions.
     * @return The centroid of the mesh.
     */
    public Vector3 getCentroid()
    {
        return centroid;
    }

    /**
     * Gets the bounding radius of the mesh - that is, the furthest distance from the centroid to a vertex position.
     * @return The centroid of the mesh.
     */
    public float getBoundingRadius()
    {
        return boundingRadius;
    }

    /**
     * Gets the center of the mesh's bounding box.
     * @return The centroid of the mesh.
     */
    public Vector3 getBoundingBoxCenter()
    {
        return boundingBoxCenter;
    }

    /**
     * Gets the size of the mesh's bounding box.
     * @return The centroid of the mesh.
     */
    public Vector3 getBoundingBoxSize()
    {
        return boundingBoxSize;
    }

    /**
     * Gets a packed list containing the tangents of the mesh that can be used by a GL.
     * @return A packed list containing the tangents.
     */
    public NativeVectorBuffer getTangents()
    {
        return tangents;
    }

    /**
     * Gets the filename of the mesh's associated material.
     * @return The name of the material file for this mesh.
     */
    public String getMaterialFileName()
    {
        return this.materialFileName;
    }

    /**
     * Gets the associated material for this mesh.
     * @return The material for this mesh.
     */
    public Material getMaterial()
    {
        return this.material;
    }

    /**
     * Gets the mesh filename.
     * @return The filename of the mesh.
     */
    public File getFilename()
    {
        return filename;
    }

    /**
     * Sets the mesh filename.
     * @param filename The new filename of the mesh.
     */
    public void setFilename(File filename)
    {
        this.filename = filename;
    }
}
