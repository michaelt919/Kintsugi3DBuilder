/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
		List<Float> vertexList = new ArrayList<Float>();
		List<Float> normalList = new ArrayList<Float>();
		List<Float> texCoordList = new ArrayList<Float>();
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
					
					vertexList.add(x);
					vertexList.add(y);
					vertexList.add(z);
				}
				else if (id.equals("vt"))
				{
					// Texture coordinate
					if (this.hasTexCoords)
					{
						texCoordList.add(scanner.nextFloat());
						texCoordList.add(scanner.nextFloat());
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
						float scale = 1.0f / (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
						normalList.add(nx * scale);
						normalList.add(ny * scale);
						normalList.add(nz * scale);
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
				}
				
				// Always advance to the next line.
				scanner.nextLine();
			}
		}
		
		centroid = sum.dividedBy(vertexList.size() / 3);
		
		// Copy the data from the dynamic tables into a data structure that OpenGL can use.
		int vertexCount = vertexIndexList.size();
		vertices = new FloatVertexList(3, vertexCount * 3);
		int i = 0;
		for (int k : vertexIndexList)
		{
			vertices.set(i, 0, vertexList.get(3 * k));
			vertices.set(i, 1, vertexList.get(3 * k + 1));
			vertices.set(i, 2, vertexList.get(3 * k + 2));
			i++;
		}
		
		if (hasNormals)
		{
			normals = new FloatVertexList(3, vertexCount * 3);
			i = 0;
			for (int k : normalIndexList)
			{
				normals.set(i, 0, normalList.get(3 * k));
				normals.set(i, 1, normalList.get(3 * k + 1));
				normals.set(i, 2, normalList.get(3 * k + 2));
				i++;
			}
		}
		
		if (hasTexCoords)
		{
			texCoords = new FloatVertexList(2, vertexCount * 3);
			i = 0;
			for (int k : texCoordIndexList)
			{
				texCoords.set(i, 0, texCoordList.get(2 * k));
				texCoords.set(i, 1, texCoordList.get(2 * k + 1));
				i++;
			}
		}
		
		System.out.println("Mesh loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
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
}
