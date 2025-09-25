/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.scene;

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public interface ReadonlyEnvironmentModel
{
    float getEnvironmentRotation();
    float getEnvironmentIntensity();

    boolean isEnvironmentMappingEnabled();

    boolean isEnabled();

    Vector3 getEnvironmentColor();
    Matrix4 getEnvironmentMapMatrix();
    float getEnvironmentMapFilteringBias();

    BackgroundMode getBackgroundMode();
    float getBackgroundIntensity();
    Vector3 getBackgroundColor();

    Vector3 getGroundPlaneColor();
    boolean isGroundPlaneEnabled();
    float getGroundPlaneHeight();
    float getGroundPlaneSize();
}
