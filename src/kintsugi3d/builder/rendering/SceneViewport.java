/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering;

import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;

public interface SceneViewport
{
    /**
     *
     * @param x In normalized [0, 1] range
     * @param y In normalized [0, 1] range
     * @return
     */
    Object getObjectAtCoordinates(double x, double y);

    /**
     *
     * @param x In normalized [0, 1] range
     * @param y In normalized [0, 1] range
     * @return
     */
    Vector3 get3DPositionAtCoordinates(double x, double y);

    /**
     *
     * @param x In normalized [0, 1] range
     * @param y In normalized [0, 1] range
     * @return
     */
    Vector3 getViewingDirection(double x, double y);

    Vector3 getViewportCenter();

    /**
     *
     * @param point
     * @return (x, y) in normalized [0, 1] range
     */
    Vector2 projectPoint(Vector3 point);
    float getLightWidgetScale();
}
