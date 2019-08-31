/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import tetzlaff.gl.vecmath.Matrix4;

/**
 * An interface for a definition of 3D to 2D projection that can be expressed as a projective transformation matrix.
 * @author Michael Tetzlaff
 *
 */
public interface Projection 
{
    /**
     * Gets the projective transformation matrix for this projection.
     * @param nearPlane The plane in 3D Cartesian space that will get mapped to the plane z=1.
     * @param farPlane The plane in 3D Cartesian space that will get mapped to the plane z=-1.
     * @return The projective transformation matrix.
     */
    Matrix4 getProjectionMatrix(float nearPlane, float farPlane);

    float getVerticalFieldOfView();
    float getAspectRatio();

    /**
     * Convert to a string designed for use in a VSET file
     */
    String toVSETString();
}
