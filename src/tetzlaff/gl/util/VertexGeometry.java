package tetzlaff.gl.util;

import java.io.File;
import java.io.IOException;
import java.util.*;

import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;

public class VertexGeometry 
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

    private String materialFileName = null;
    private Material material = null; // TODO support multiple materials

    private VertexGeometry(File filename)
    {
        this.filename = filename;
    }

    /**
     * Initializes the mesh from a file containing the mesh in Wavefront OBJ format.
     */
    public static VertexGeometry createFromOBJFile(File file) throws IOException
    {
        VertexGeometry inst = new VertexGeometry(file);

        Date timestamp = new Date();

        // Assume initially that normals and texture coordinates are present
        inst.hasNormals = true;
        inst.hasTexCoords = true;

        // Initialize dynamic tables to store the data from the file
        List<Vector3> vertexList = new ArrayList<Vector3>();
        List<Vector3> normalList = new ArrayList<Vector3>();
        List<Vector3> tangentList = new ArrayList<Vector3>();
        List<Vector3> bitangentList = new ArrayList<Vector3>();
        List<Vector2> texCoordList = new ArrayList<Vector2>();
        List<Integer> vertexIndexList = new ArrayList<Integer>();
        List<Integer> normalIndexList = new ArrayList<Integer>();
        List<Integer> texCoordIndexList = new ArrayList<Integer>();

        Vector3 sum = Vector3.ZERO;

        String materialName = null;

        try(Scanner scanner = new Scanner(file))
        {
            while(scanner.hasNext())
            {
                String id = scanner.next();
                if (id.equals("mtllib"))
                {
                    if (inst.materialFileName == null)
                    {
                        // Use first material filename found
                        inst.materialFileName = scanner.next();
                    }
                }
                else if (id.equals("usemtl"))
                {
                    if (materialName == null)
                    {
                        // Use first material found
                        materialName = scanner.next();
                    }
                }
                else if (id.equals("v"))
                {
                    // Vertex position
                    float x = scanner.nextFloat();
                    float y = scanner.nextFloat();
                    float z = scanner.nextFloat();

                    sum = sum.plus(new Vector3(x,y,z));

                    vertexList.add(new Vector3(x,y,z));
                }
                else if (id.equals("vt"))
                {
                    // Texture coordinate
                    if (inst.hasTexCoords)
                    {
                        texCoordList.add(new Vector2(scanner.nextFloat(), scanner.nextFloat()));
                    }
                }
                else if (id.equals("vn"))
                {
                    if (inst.hasNormals)
                    {
                        // Vertex normal
                        float nx = scanner.nextFloat();
                        float ny = scanner.nextFloat();
                        float nz = scanner.nextFloat();

                        // Normalize to unit length
                        normalList.add(new Vector3(nx, ny, nz).normalized());

                        tangentList.add(new Vector3(0.0f, 0.0f, 0.0f));
                        bitangentList.add(new Vector3(0.0f, 0.0f, 0.0f));
                    }
                }
                else if (id.equals("f"))
                {
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

                            int normalIndex0 = normalIndexList.get(normalIndexList.size() - 3);
                            int normalIndex1 = normalIndexList.get(normalIndexList.size() - 2);
                            int normalIndex2 = normalIndexList.get(normalIndexList.size() - 1);

                            tangentList.set(normalIndex0, tangentList.get(normalIndex0).plus(tangents[0]));
                            tangentList.set(normalIndex1, tangentList.get(normalIndex1).plus(tangents[0]));
                            tangentList.set(normalIndex2, tangentList.get(normalIndex2).plus(tangents[0]));

                            bitangentList.set(normalIndex0, bitangentList.get(normalIndex0).plus(tangents[1]));
                            bitangentList.set(normalIndex1, bitangentList.get(normalIndex1).plus(tangents[1]));
                            bitangentList.set(normalIndex2, bitangentList.get(normalIndex2).plus(tangents[1]));
                        }
                        else
                        {
                            // TODO
                        }
                    }
                }

                // Always advance to the next line.
                scanner.nextLine();
            }
        }

        inst.centroid = sum.dividedBy(vertexList.size());

        List<Vector4> orthoTangentsList = new ArrayList<Vector4>();

        // Normalize and orthogonalize tangent vectors
        for (int i = 0; i < tangentList.size(); i++)
        {
            orthoTangentsList.add(orthogonalizeTangent(normalList.get(i), tangentList.get(i), bitangentList.get(i)));
        }

        float boundingBoxMinX = 0.0f, boundingBoxMinY = 0.0f, boundingBoxMinZ = 0.0f,
                boundingBoxMaxX = 0.0f, boundingBoxMaxY = 0.0f, boundingBoxMaxZ = 0.0f;
        inst.boundingRadius = 0.0f;

        // Copy the data from the dynamic tables into a data structure that OpenGL can use.
        int vertexCount = vertexIndexList.size();
        inst.vertices = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 3, vertexCount * 3);
        int i = 0;
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

            inst.vertices.set(i, 0, vertex.x);
            inst.vertices.set(i, 1, vertex.y);
            inst.vertices.set(i, 2, vertex.z);

            i++;
        }

        inst.boundingBoxCenter = new Vector3((boundingBoxMinX + boundingBoxMaxX) / 2, (boundingBoxMinY + boundingBoxMaxY) / 2, (boundingBoxMinZ + boundingBoxMaxZ) / 2);
        inst.boundingBoxSize = new Vector3(boundingBoxMaxX - boundingBoxMinX, boundingBoxMaxY - boundingBoxMinY, boundingBoxMaxZ - boundingBoxMinZ);

        if (inst.hasNormals)
        {
            inst.normals = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 3, vertexCount * 3);
            i = 0;
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
            inst.texCoords = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 2, vertexCount * 3);
            i = 0;
            for (int k : texCoordIndexList)
            {
                inst.texCoords.set(i, 0, texCoordList.get(k).x);
                inst.texCoords.set(i, 1, texCoordList.get(k).y);
                i++;
            }
        }

        if (inst.hasTexCoords && inst.hasNormals)
        {
            inst.tangents = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, vertexCount * 3);
            i = 0;
            for (int k : normalIndexList)
            {
                inst.tangents.set(i, 0, orthoTangentsList.get(k).x);
                inst.tangents.set(i, 1, orthoTangentsList.get(k).y);
                inst.tangents.set(i, 2, orthoTangentsList.get(k).z);
                inst.tangents.set(i, 3, orthoTangentsList.get(k).w);
                i++;
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

    public boolean hasNormals()
    {
        return hasNormals;
    }

    public boolean hasTexCoords()
    {
        return hasTexCoords;
    }

    public NativeVectorBuffer getVertices()
    {
        return vertices;
    }

    public NativeVectorBuffer getNormals()
    {
        return normals;
    }

    public NativeVectorBuffer getTexCoords()
    {
        return texCoords;
    }

    public Vector3 getCentroid()
    {
        return centroid;
    }

    public float getBoundingRadius()
    {
        return boundingRadius;
    }

    public Vector3 getBoundingBoxCenter()
    {
        return boundingBoxCenter;
    }

    public Vector3 getBoundingBoxSize()
    {
        return boundingBoxSize;
    }

    public NativeVectorBuffer getTangents()
    {
        return tangents;
    }

    public String getMaterialFileName()
    {
        return this.materialFileName;
    }

    public Material getMaterial()
    {
        return this.material;
    }

    public File getFilename()
    {
        return filename;
    }

    public void setFilename(File filename)
    {
        this.filename = filename;
    }
}
