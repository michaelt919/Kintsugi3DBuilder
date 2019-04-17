/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.util;

import umn.gl.vecmath.Vector3;

/**
 * An extension of the VertexGeometry interface that provides additional statistics about the size and center of the geometry.
 */
public interface BoundedVertexGeometry extends VertexGeometry
{
    /**
     * Gets the centroid, that is, the average vertex position for the geometry.
     * @return The centroid of the geometry.
     */
    Vector3 getCentroid();

    /**
     * Gets a bounding radius around the centroid.  All vertices are guaranteed to fall within this radius of the centroid.
     * This bound should be as tight as possible within reasonable round-off limitations.
     * @return The bounding radius of the geometry.
     */
    float getBoundingRadius();

    /**
     * Gets the center of a bounding box containing the object.  This is not necessarily the mathematical centroid.
     * @return The center of the bounding box.
     */
    Vector3 getBoundingBoxCenter();

    /**
     * Gets the dimensions (width, height, and depth) of a bounding box containing the object.
     * @return The dimensions of the bounding box.
     */
    Vector3 getBoundingBoxSize();
}
