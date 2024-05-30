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

package kintsugi3d.gl.geometry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import javafx.util.Pair;
import org.jengineering.sjmply.PLY;
import org.jengineering.sjmply.PLYElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.material.Material;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

import static org.jengineering.sjmply.PLYType.*;

/**
 * A data structure for representing a geometry mesh consisting of vertex positions, surface normals, and texture coordinates.
 * @author Michael Tetzlaff
 *
 */
public final class VertexGeometry implements ReadonlyVertexGeometry
{
    private static final Logger log = LoggerFactory.getLogger(VertexGeometry.class);
    private static File geometryFile;
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
     * Initializes the mesh from a file containing the mesh in Wavefront OBJ format OR in PLY format
     * @param geometryFile
     * @throws IOException
     */
    public static VertexGeometry createFromGeometryFile(File geometryFile) throws IOException
    {
        VertexGeometry.geometryFile = geometryFile;
        String fileName = geometryFile.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        if ("zip".equalsIgnoreCase(fileExtension)){
            //TODO: set VertexGeometry.geometryFile to unzipped file?
            return VertexGeometry.createFromZippedPLYFile(geometryFile, "mesh.ply");
        } else if("obj".equalsIgnoreCase(fileExtension)) {
             return VertexGeometry.createFromOBJFile(geometryFile);
        }else if("ply".equalsIgnoreCase(fileExtension)){
            return VertexGeometry.createFromPLYFile(geometryFile);
        }else{
            return null;
        }
    }

    /**
     * Initializes the mesh from a stream containing the mesh in Wavefront OBJ format.
     * @param stream The stream to load.
     * @throws FileNotFoundException Thrown if any File I/O errors occur.
     */
    public static VertexGeometry createFromOBJStream(InputStream stream) throws FileNotFoundException
    {
        try(Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8))
        {
            scanner.useLocale(Locale.US);
            return createFromOBJ(null, scanner);
        }
    }

    /**
     * Initializes the mesh from a file containing the mesh in Wavefront OBJ format.
     * @param file The file to load.
     * @throws FileNotFoundException Thrown if any File I/O errors occur.
     */
    public static VertexGeometry createFromOBJFile(File file) throws IOException
    {
        try(Scanner scanner = new Scanner(file, StandardCharsets.UTF_8))
        {
            scanner.useLocale(Locale.US);
            return createFromOBJ(file, scanner);
        }
    }

    private static VertexGeometry createFromOBJ(File file, Scanner scanner)
    {
        Date timestamp = new Date();

        // Assume initially that normals and texture coordinates are present
        boolean hasNormals = true;
        boolean hasTexCoords = true;
        String materialFileName = null;

        // Initialize dynamic tables to store the data from the file
        List<Vector3> vertexList = new ArrayList<>(100000);
        List<Vector3> normalList = new ArrayList<>(100000);
        List<Vector2> texCoordList = new ArrayList<>(100000);
        List<Integer> vertexIndexList = new ArrayList<>(100000);
        List<Integer> normalIndexList = new ArrayList<>(100000);
        List<Integer> texCoordIndexList = new ArrayList<>(100000);

        Vector3 sum = Vector3.ZERO;

        String materialName = null;

        while(scanner.hasNext())
        {
            String id = scanner.next();
            switch (id)
            {
                case "mtllib":
                    if (materialFileName == null)
                    {
                        // Use first material filename found
                        materialFileName = scanner.next();
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
                    if (hasTexCoords)
                    {
                        texCoordList.add(new Vector2(scanner.nextFloat(), scanner.nextFloat()));
                    }
                    break;
                case "vn":
                    if (hasNormals)
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
                            hasTexCoords = false;
                        }
                        else if (hasTexCoords)
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
                            hasNormals = false;
                        }
                        else if (hasNormals)
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
                    break;
                default:
                    break;
            }

            // Always advance to the next line.
            scanner.nextLine();
        }

        VertexGeometry inst = createFromArrays(file, hasNormals, hasTexCoords, vertexList, vertexIndexList, normalList,
                normalIndexList, texCoordList, texCoordIndexList);

        // Handle OBJ materials
        inst.materialFileName = materialFileName;
        if (file != null && inst.materialFileName != null)
        {
            try
            {
                Dictionary<String, Material> materialLibrary = Material.loadFromMTLFile(new File(file.getParentFile(), inst.materialFileName));
                inst.material = materialLibrary.get(materialName);
            }
            catch(IOException e)
            {
                log.error("IO Exception while loading material:", e);
                inst.material = null;
            }
        }
        else
        {
            inst.material = null;
        }

        log.info("Mesh loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        return inst;
    }

    /**
     * Initializes the mesh from a stream containing the mesh in Wavefront OBJ format.
     * @param stream The stream to load.
     * @throws FileNotFoundException Thrown if any File I/O errors occur.
     */
    public static VertexGeometry createFromPLYStream(InputStream stream) throws IOException
    {
        return createFromPLY(null, PLY.read(stream));
    }

    /**
     * Initializes the mesh from a file containing the mesh in Wavefront OBJ format.
     * @param file The file to load.
     * @throws FileNotFoundException Thrown if any File I/O errors occur.
     */
    public static VertexGeometry createFromPLYFile(File file) throws IOException
    {
        return createFromPLY(file, PLY.load(file.toPath()));
    }
    public static VertexGeometry createFromZippedPLYFile(File zipFolder, String targetFileName) throws IOException
    {
        return createFromPLY(zipFolder, PLY.loadFromZip(zipFolder, targetFileName));
    }

    private static VertexGeometry createFromPLY(File file, PLY ply) throws IOException
    {
        PLYElementList vertex = ply.elements("vertex");
        boolean hasNormals = false, hasTexCoords = false;

        List<Vector3> vertexList = new ArrayList<>(100000);
        List<Vector3> normalList = new ArrayList<>(100000);
        List<Vector2> texCoordList = new ArrayList<>(100000);
        List<Integer> vertexIndexList = new ArrayList<>(100000);
        List<Integer> normalIndexList = new ArrayList<>(100000);
        List<Integer> texCoordIndexList = new ArrayList<>(100000);
        Map<NormalTexCoordPair, Vector3> tangentMap = new HashMap<>(100000);
        Map<NormalTexCoordPair, Vector3> bitangentMap = new HashMap<>(100000);

        // Collect all values stored per-vertex
        vertex.convertProperty("x", FLOAT32);
        vertex.convertProperty("y", FLOAT32);
        vertex.convertProperty("z", FLOAT32);
        float[] x = vertex.property(FLOAT32, "x");
        float[] y = vertex.property(FLOAT32, "y");
        float[] z = vertex.property(FLOAT32, "z");

        float[] nx = {};
        float[] ny = {};
        float[] nz = {};

        float[] s = {};
        float[] t = {};

        if (vertex.properties.keySet().containsAll(List.of(new String[]{"nx", "ny", "nz"})))
        {
            hasNormals = true;

            vertex.convertProperty("nx", FLOAT32);
            vertex.convertProperty("ny", FLOAT32);
            vertex.convertProperty("nz", FLOAT32);
            nx = vertex.property(FLOAT32, "nx");
            ny = vertex.property(FLOAT32, "ny");
            nz = vertex.property(FLOAT32, "nz");
        }

        if (vertex.properties.keySet().containsAll(List.of(new String[]{"s", "t"})))
        {
            hasTexCoords = true;

            vertex.convertProperty("s", FLOAT32);
            vertex.convertProperty("t", FLOAT32);
            s = vertex.property(FLOAT32, "s");
            t = vertex.property(FLOAT32, "t");
        }

        for (int i = 0; i < vertex.size; i++)
        {
            vertexList.add(new Vector3(x[i], y[i], z[i]));

            if (hasNormals)
            {
                normalList.add(new Vector3(nx[i], ny[i], nz[i]));
            }

            if (hasTexCoords)
            {
                texCoordList.add(new Vector2(s[i], t[i]));
            }
        }

        // Load properties stored per-face
        PLYElementList face = ply.elements("face");
        face.convertProperty("vertex_indices", LIST(UINT32,INT32));

        int[][] vertex_indices = face.property(LIST(UINT32,INT32),"vertex_indices");

        boolean facialTexCoords = !hasTexCoords && face.properties.containsKey("texcoord");
        float[][] faceCoords = {};
        if (facialTexCoords)
        {
            hasTexCoords = true;
            face.convertProperty("texcoord", LIST(UINT32,FLOAT32));
            faceCoords = face.property(LIST(UINT32, FLOAT32), "texcoord");
        }

        for (int i = 0; i < face.size; i++)
        {
            for (int v = 0; v < 3; v++)
            {
                int vertexIndex = vertex_indices[i][v];
                vertexIndexList.add(vertexIndex);

                if (hasNormals)
                {
                    normalIndexList.add(vertexIndex);
                }

                if (hasTexCoords)
                {
                    if (facialTexCoords)
                    {
                        texCoordList.add(new Vector2(faceCoords[i][v*2], faceCoords[i][(v*2)+1]));
                    }

                    texCoordIndexList.add(vertexIndex);
                }
            }
        }

        VertexGeometry inst = createFromArrays(file, hasNormals, hasTexCoords, vertexList, vertexIndexList, normalList,
                normalIndexList, texCoordList, texCoordIndexList);

        return inst;
    }

    private static VertexGeometry createFromArrays(
        File file, boolean hasNormals, boolean hasTexCoords, List<Vector3> vertexList, List<Integer> vertexIndexList,
        List<Vector3> normalList, List<Integer> normalIndexList, List<Vector2> texCoordList, List<Integer> texCoordIndexList)
    {
        VertexGeometry inst = new VertexGeometry(file);
        inst.hasNormals = hasNormals;
        inst.hasTexCoords = hasTexCoords;

        // Compute smooth normals if applicable
        if (!inst.hasNormals)
        {
            Pair<List<Vector3>, List<Integer>> norms = computeNormals(vertexList, vertexIndexList);
            normalList = norms.getKey();
            normalIndexList = norms.getValue();
            inst.hasNormals = true;
        }

        Vector3 sum = Vector3.ZERO;
        Map<NormalTexCoordPair, Vector3> tangentMap = new HashMap<>(vertexIndexList.size());
        Map<NormalTexCoordPair, Vector3> bitangentMap = new HashMap<>(vertexIndexList.size());

        for (int f = 0; f < vertexIndexList.size() - 2; f += 3)
        {
            if (inst.hasTexCoords)
            {
                Vector3 position0 = vertexList.get(vertexIndexList.get(f));
                Vector3 position1 = vertexList.get(vertexIndexList.get(f + 1));
                Vector3 position2 = vertexList.get(vertexIndexList.get(f + 2));

                // Add positions to sum to compute centroid
                sum = sum.plus(position0);
                sum = sum.plus(position1);
                sum = sum.plus(position2);

                Vector2 texCoords0 = texCoordList.get(texCoordIndexList.get(f));
                Vector2 texCoords1 = texCoordList.get(texCoordIndexList.get(f + 1));
                Vector2 texCoords2 = texCoordList.get(texCoordIndexList.get(f + 2));

                Vector3[] tangents = computeTangents(position0, position1, position2, texCoords0, texCoords1, texCoords2);

                // TODO broken code - make it so that two vertices share tangents if they share normals AND texture coordinates

                NormalTexCoordPair pair0 = new NormalTexCoordPair(normalIndexList.get(f), texCoordIndexList.get(f));
                NormalTexCoordPair pair1 = new NormalTexCoordPair(normalIndexList.get(f + 1), texCoordIndexList.get(f + 1));
                NormalTexCoordPair pair2 = new NormalTexCoordPair(normalIndexList.get(f + 2), texCoordIndexList.get(f + 2));

                tangentMap.put(pair0, tangentMap.getOrDefault(pair0, Vector3.ZERO).plus(tangents[0]));
                tangentMap.put(pair1, tangentMap.getOrDefault(pair1, Vector3.ZERO).plus(tangents[0]));
                tangentMap.put(pair2, tangentMap.getOrDefault(pair2, Vector3.ZERO).plus(tangents[0]));

                bitangentMap.put(pair0, bitangentMap.getOrDefault(pair0, Vector3.ZERO).plus(tangents[1]));
                bitangentMap.put(pair1, bitangentMap.getOrDefault(pair1, Vector3.ZERO).plus(tangents[1]));
                bitangentMap.put(pair2, bitangentMap.getOrDefault(pair2, Vector3.ZERO).plus(tangents[1]));
            }
        }

        inst.centroid = sum.dividedBy(vertexIndexList.size());

        Map<NormalTexCoordPair, Vector4> orthoTangentsMap = new HashMap<>(tangentMap.size());
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

        return inst;
    }

    private static Pair<List<Vector3>, List<Integer>> computeNormals(List<Vector3> vertexList, List<Integer> vertexIndexList)
    {
        List<Vector3> normals = new ArrayList<>(Collections.nCopies(vertexList.size(), Vector3.ZERO));
        List<Integer> normalIndexList = new ArrayList<>(vertexIndexList.size());

        // Iterate over faces
        for (int i = 0; i < vertexIndexList.size() - 2; i+=3)
        {
            int i1 = vertexIndexList.get(i);
            int i2 = vertexIndexList.get(i + 1);
            int i3 = vertexIndexList.get(i + 2);

            Vector3 p1 = vertexList.get(i1);
            Vector3 p2 = vertexList.get(i2);
            Vector3 p3 = vertexList.get(i3);

            Vector3 facialNorm = (p2.minus(p1)).cross(p3.minus(p1));

            // Calculate angle weights
            float angle1 = (float) Math.acos(p2.minus(p1).normalized().dot(p3.minus(p1).normalized()));
            float angle2 = (float) Math.acos(p3.minus(p1).normalized().dot(p1.minus(p2).normalized()));
            float angle3 = (float) Math.acos(p1.minus(p3).normalized().dot(p2.minus(p3).normalized()));

            normals.set(i1, normals.get(i1).plus(facialNorm.times(angle1)));
            normals.set(i2, normals.get(i2).plus(facialNorm.times(angle2)));
            normals.set(i3, normals.get(i3).plus(facialNorm.times(angle3)));

            normalIndexList.add(i1);
            normalIndexList.add(i2);
            normalIndexList.add(i3);
        }

        // Normalize summed facial normals
        for (int i = 0; i < normals.size(); i++)
        {
            normals.set(i, normals.get(i).normalized());
        }

        return new Pair<>(normals, normalIndexList);
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

    @Override
    public boolean hasNormals()
    {
        return hasNormals;
    }

    @Override
    public boolean hasTexCoords()
    {
        return hasTexCoords;
    }

    @Override
    public <ContextType extends Context<ContextType>> GeometryResources<ContextType> createGraphicsResources(ContextType context)
    {
        return new GeometryResources<>(context, this);
    }

    @Override
    public ReadonlyNativeVectorBuffer getVertices()
    {
        return vertices;
    }

    @Override
    public ReadonlyNativeVectorBuffer getNormals()
    {
        return normals;
    }

    @Override
    public ReadonlyNativeVectorBuffer getTexCoords()
    {
        return texCoords;
    }

    @Override
    public Vector3 getCentroid()
    {
        return centroid;
    }

    @Override
    public float getBoundingRadius()
    {
        return boundingRadius;
    }

    @Override
    public Vector3 getBoundingBoxCenter()
    {
        return boundingBoxCenter;
    }

    @Override
    public Vector3 getBoundingBoxSize()
    {
        return boundingBoxSize;
    }

    @Override
    public ReadonlyNativeVectorBuffer getTangents()
    {
        return tangents;
    }

    @Override
    public String getMaterialFileName()
    {
        return this.materialFileName;
    }

    @Override
    public Material getMaterial()
    {
        return this.material;
    }

    @Override
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
