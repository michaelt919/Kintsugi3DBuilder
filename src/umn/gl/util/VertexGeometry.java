/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.util;

import java.io.File;

import umn.gl.material.Material;
import umn.gl.nativebuffer.NativeVectorBuffer;

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