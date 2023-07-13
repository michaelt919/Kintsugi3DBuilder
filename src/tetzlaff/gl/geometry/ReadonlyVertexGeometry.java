/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.geometry;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.material.ReadonlyMaterial;
import tetzlaff.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import tetzlaff.gl.vecmath.Vector3;

import java.io.File;
import java.nio.ByteBuffer;

public interface ReadonlyVertexGeometry
{
    /**
     * Gets whether or not the mesh has surface normals.
     * @return true if the mesh has surface normals, false otherwise.
     */
    boolean hasNormals();

    /**
     * Gets whether or not the mesh has texture coordinates.
     * @return true if the mesh has texture coordinates, false otherwise.
     */
    boolean hasTexCoords();

    <ContextType extends Context<ContextType>> GeometryResources<ContextType> createGraphicsResources(ContextType context);

    /**
     * Gets a packed list containing the vertex positions of the mesh that can be used by a GL.
     *
     * @return A packed list containing the vertex positions.
     */
    ReadonlyNativeVectorBuffer getVertices();

    /**
     * Gets a packed list containing the surface normals of the mesh that can be used by a GL.
     *
     * @return A packed list containing the surface normals.
     */
    ReadonlyNativeVectorBuffer getNormals();

    /**
     * Gets a packed list containing the texture coordinates of the mesh that can be used by a GL.
     *
     * @return A packed list containing the texture coordinates.
     */
    ReadonlyNativeVectorBuffer getTexCoords();

    /**
     * Gets the centroid of the mesh - that is, the average of all the vertex positions.
     * @return The centroid of the mesh.
     */
    Vector3 getCentroid();

    /**
     * Gets the bounding radius of the mesh - that is, the furthest distance from the centroid to a vertex position.
     * @return The centroid of the mesh.
     */
    float getBoundingRadius();

    /**
     * Gets the center of the mesh's bounding box.
     * @return The centroid of the mesh.
     */
    Vector3 getBoundingBoxCenter();

    /**
     * Gets the size of the mesh's bounding box.
     * @return The centroid of the mesh.
     */
    Vector3 getBoundingBoxSize();

    /**
     * Gets a packed list containing the tangents of the mesh that can be used by a GL.
     *
     * @return A packed list containing the tangents.
     */
    ReadonlyNativeVectorBuffer getTangents();

    /**
     * Gets the filename of the mesh's associated material.
     * @return The name of the material file for this mesh.
     */
    String getMaterialFileName();

    /**
     * Gets the associated material for this mesh.
     * @return The material for this mesh.
     */
    ReadonlyMaterial getMaterial();

    /**
     * Gets the mesh filename.
     * @return The filename of the mesh.
     */
    File getFilename();
}
