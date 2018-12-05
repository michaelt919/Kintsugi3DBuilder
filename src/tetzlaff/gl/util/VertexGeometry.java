package tetzlaff.gl.util;

import java.io.File;

import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

/**
 * An interface that provides access to a geometric description of an object or surface.
 * An implementation of this interface must be able to provide in-memory buffers containing vertex information that can be passed to vertex buffers
 * in a graphics context.
 */
public interface VertexGeometry
{
    /**
     * Gets whether or not the geometry has texture coordinates defined.
     * @return true if texture coordinates are defined; false otherwise.
     */
    boolean hasTexCoords();

    /**
     * Gets whether or not the geometry has surface normals defined.
     * @return true if surface normals are defined; false otherwise.
     */
    boolean hasNormals();

    /**
     * Gets the vertex positions in a buffer that can be passed to a vertex buffer in a graphics context.
     * @return A buffer containing the vertex positions.
     */
    NativeVectorBuffer getVertices();

    /**
     * Gets the texture coordinates in a buffer that can be passed to a vertex buffer in a graphics context.
     * @return A buffer containing the texture coordinates.
     */
    NativeVectorBuffer getTexCoords();

    /**
     * Gets the surface normals in a buffer that can be passed to a vertex buffer in a graphics context.
     * @return A buffer containing the surface normals.
     */
    NativeVectorBuffer getNormals();

    /**
     * Gets the surface tangents in a buffer that can be passed to a vertex buffer in a graphics context.
     * The geometry must have both texture coordinates and surface normals defined in order to have surface tangents.
     * @return A buffer containing the surface tangents.
     */
    NativeVectorBuffer getTangents();

    /**
     * Gets the material associated with this geometry.
     * @return The material object.
     */
    Material getMaterial();

    /**
     * Gets the name of the file for the material associated with this geometry.
     * @return The name of the material file.
     */
    String getMaterialFileName();

    /**
     * Gets the name of the file from which this geometry object originated.
     * @return The geometry file name.
     */
    File getFilename();

    /**
     * Sets the name of the file from which this geometry object originated.
     * @param filename The geometry file name.
     */
    void setFilename(File filename);
}
