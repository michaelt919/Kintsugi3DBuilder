/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.SceneViewport;
import tetzlaff.models.SceneViewportModel;

public class SceneViewportModelImpl implements SceneViewportModel
{
    private static final SceneViewport SENTINEL = new SceneViewport()
    {
        @Override
        public Object getObjectAtCoordinates(double x, double y)
        {
            return null;
        }

        @Override
        public Vector3 get3DPositionAtCoordinates(double x, double y)
        {
            return Vector3.ZERO;
        }

        @Override
        public Vector3 getViewingDirection(double x, double y)
        {
            return Vector3.ZERO;
        }

        @Override
        public Vector3 getViewportCenter()
        {
            return Vector3.ZERO;
        }

        @Override
        public Vector2 projectPoint(Vector3 point)
        {
            return Vector2.ZERO;
        }

        @Override
        public float getLightWidgetScale()
        {
            return 1.0f;
        }
    };

    private SceneViewport sceneViewport = SENTINEL;

    @Override
    public SceneViewport getSceneViewport()
    {
        return sceneViewport;
    }

    @Override
    public void setSceneViewport(SceneViewport sceneViewport)
    {
        this.sceneViewport = sceneViewport == null ? SENTINEL : sceneViewport;
    }
}
