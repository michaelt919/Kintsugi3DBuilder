package tetzlaff.gl.helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import tetzlaff.helpers.ZipWrapper;

/**
 * A data structure for representing a vertex mesh consisting of vertex positions, surface normals, and texture coordinates.
 * @author Michael Tetzlaff
 *
 */
public class VertexMesh 
{
	/**
	 * Whether or not the vertex mesh has surface normals.
	 */
	private boolean hasNormals;
	
	/**
	 * Whether or not the vertex mesh has texture coordinates.
	 */
	private boolean hasTexCoords;
	
	/**
	 * A packed list storing the vertex positions of the mesh.
	 */
	private FloatVertexList vertices;
	
	/**
	 * A packed list storing the surface normals of the mesh.
	 */
	private FloatVertexList normals;
	
	/**
	 * A packed list storing the texture coordinates of the mesh.
	 */
	private FloatVertexList texCoords;
	
	/**
	 * The centroid of the mesh - that is, the average of all the vertex positions.
	 */
	private Vector3 centroid;
	
	/**
	 * The tangent vectors
	 */
	private FloatVertexList tangents;

	/**
	 * Loads a new vertex mesh from a file.
	 * @param fileFormat The file format.  Currently only Wavefront OBJ files are supported (fileFormat="OBJ").
	 * A runtime exception will be thrown if an unsupported file format is specified.
	 * @param file The file to load.
	 * @throws IOException Thrown if any File I/O errors occur.
	 */
	public VertexMesh(String fileFormat, File file) throws IOException
	{
		ZipWrapper myZip = new ZipWrapper(file);
		InputStream inputStream = myZip.getInputStream();
		if (fileFormat.equals("OBJ"))
		{
			initFromOBJStream(inputStream);
		}
		else
		{
			throw new IllegalArgumentException("Invalid file format.");
		}
	}

	/**
	 * Initializes the mesh from a text stream containing the mesh in Wavefront OBJ format.
	 * @param objStream The stream containing the mesh in Wavefront OBJ format.
	 */
	private void initFromOBJStream(InputStream objStream)
	{
		Date timestamp = new Date();
		
		// Assume initially that normals and texture coordinates are present
		this.hasNormals = true;
		this.hasTexCoords = true;
		
		// Initialize dynamic tables to store the data from the file
		List<Vector3> vertexList = new ArrayList<Vector3>();
		List<Vector3> normalList = new ArrayList<Vector3>();
		List<Vector3> tangentList = new ArrayList<Vector3>();
		List<Vector3> bitangentList = new ArrayList<Vector3>();
		List<Vector2> texCoordList = new ArrayList<Vector2>();
		List<Integer> vertexIndexList = new ArrayList<Integer>();
		List<Integer> normalIndexList = new ArrayList<Integer>();
		List<Integer> texCoordIndexList = new ArrayList<Integer>();
		
		Vector3 sum = new Vector3(0.0f, 0.0f, 0.0f);
		
		try(Scanner scanner = new Scanner(objStream))
		{
			while(scanner.hasNext())
			{
				String id = scanner.next();
				if (id.equals("v"))
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
					if (this.hasTexCoords)
					{
						texCoordList.add(new Vector2(scanner.nextFloat(), scanner.nextFloat()));
					}
				}
				else if (id.equals("vn"))
				{
					if (this.hasNormals)
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
							this.hasTexCoords = false;
						}
						else if (this.hasTexCoords)
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
							this.hasNormals = false;
						}
						else if (this.hasNormals)
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
					
					if (this.hasTexCoords)
					{
						if (this.hasNormals)
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
		
		centroid = sum.dividedBy(vertexList.size() / 3);
		
		ArrayList<Vector4> orthoTangentsList = new ArrayList<Vector4>();
		
		// Normalize and orthogonalize tangent vectors
		for (int i = 0; i < tangentList.size(); i++)
		{
			orthoTangentsList.add(orthogonalizeTangent(normalList.get(i), tangentList.get(i), bitangentList.get(i)));
		}
		
		// Copy the data from the dynamic tables into a data structure that OpenGL can use.
		int vertexCount = vertexIndexList.size();
		vertices = new FloatVertexList(3, vertexCount * 3);
		int i = 0;
		for (int k : vertexIndexList)
		{
			vertices.set(i, 0, vertexList.get(k).x);
			vertices.set(i, 1, vertexList.get(k).y);
			vertices.set(i, 2, vertexList.get(k).z);
			i++;
		}
		
		if (hasNormals)
		{
			normals = new FloatVertexList(3, vertexCount * 3);
			i = 0;
			for (int k : normalIndexList)
			{
				normals.set(i, 0, normalList.get(k).x);
				normals.set(i, 1, normalList.get(k).y);
				normals.set(i, 2, normalList.get(k).z);
				i++;
			}
		}
		
		if (hasTexCoords)
		{
			texCoords = new FloatVertexList(2, vertexCount * 3);
			i = 0;
			for (int k : texCoordIndexList)
			{
				texCoords.set(i, 0, texCoordList.get(k).x);
				texCoords.set(i, 1, texCoordList.get(k).y);
				i++;
			}
		}
		
		if (hasTexCoords && hasNormals)
		{
			tangents = new FloatVertexList(4, vertexCount * 3);
			i = 0;
			for (int k : normalIndexList)
			{
				tangents.set(i, 0, orthoTangentsList.get(k).x);
				tangents.set(i, 1, orthoTangentsList.get(k).y);
				tangents.set(i, 2, orthoTangentsList.get(k).z);
				tangents.set(i, 3, orthoTangentsList.get(k).w);
				i++;
			}
		}
		
		System.out.println("Mesh loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	}
	
	private Vector3[] computeTangents(
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
	
	private Vector4 orthogonalizeTangent(Vector3 normal, Vector3 tangent, Vector3 bitangent)
	{
		Vector3 orthoTangent;
		Vector3 orthoBitangent;
		
		// Normal vector is assumed to already be normalized
		orthoTangent = tangent.minus(normal.times(normal.dot(tangent))).normalized();
		orthoBitangent = bitangent.minus(normal.times(normal.dot(bitangent)).minus(orthoTangent.times(orthoTangent.dot(bitangent)))).normalized();
		
		// dot product with normal gets value of 0 or really small e-7
		// There is at least one value of Bitangent that is NaN. 
		//System.out.println( "tangent" + orthoTangent.dot(normal) ); 
		//System.out.println( "Bitangent" + orthoBitangent.dot(normal) );
		if (Math.abs(orthoTangent.dot(normal)) > 10E-6)
		{
			System.out.println( "Tangent not 0" );
		}
		if (Math.abs(orthoBitangent.dot(normal)) > 10E-6)
		{
			System.out.println( "Bitangent not 0" );
		}
		
		return new Vector4(orthoTangent, orthoBitangent.dot(normal.cross(orthoTangent)));
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

	/**
	 * Gets a packed list containing the vertex positions of the mesh that can be used by a GL.
	 * @return A packed list containing the vertex positions.
	 */
	public FloatVertexList getVertices() 
	{
		return vertices;
	}

	/**
	 * Gets a packed list containing the surface normals of the mesh that can be used by a GL.
	 * @return A packed list containing the surface normals.
	 */
	public FloatVertexList getNormals() 
	{
		return normals;
	}

	/**
	 * Gets a packed list containing the texture coordinates of the mesh that can be used by a GL.
	 * @return A packed list containing the texture coordinates.
	 */
	public FloatVertexList getTexCoords() 
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
	
	public FloatVertexList getTangents()
	{
		return tangents;
	}
	
}
