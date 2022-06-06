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

package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;

public interface ReadonlyExtendedCameraModel extends ReadonlyCameraModel
{
    /**
     * This method is intended to return whether or not the selected camera is locked.
     * It is called by the render side of the program, and when it returns true
     * the camera should not be able to be changed using the tools in the render window.
     * @return true for locked
     */
    boolean isLocked();

    Matrix4 getOrbit();
    float getLog10Distance();
    float getDistance();
    float getTwist();
    float getAzimuth();
    float getInclination();
    float getFocalLength();

    @Override
    float getHorizontalFOV();
}
