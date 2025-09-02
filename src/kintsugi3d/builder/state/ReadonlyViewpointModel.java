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

package kintsugi3d.builder.state;

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

@FunctionalInterface
public interface ReadonlyViewpointModel
{
    Matrix4 getLookMatrix();

    default Vector3 getTarget()
    {
        return Vector3.ZERO;
    }

    default float getHorizontalFOV()
    {
        return (float)(360 / Math.PI /* convert and multiply by 2) */ * Math.atan(0.36 /* "35 mm" film (actual 36mm horizontal), 50mm lens */));
    }

    default boolean isOrthographic()
    {
        return false;
    }
}
