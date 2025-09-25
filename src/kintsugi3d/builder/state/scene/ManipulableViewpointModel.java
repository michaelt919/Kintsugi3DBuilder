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

public interface ManipulableViewpointModel extends ViewpointModel, ReadonlyManipulableViewpointModel
{
    @Override
    float getHorizontalFOV();

    void setOrbit(Matrix4 orbit);
    void setLog10Distance(float log10Distance);
    void setDistance(float distance);
    void setTwist(float twist);
    void setAzimuth(float azimuth);
    void setInclination(float inclination);
    void setFocalLength(float focalLength);
}
