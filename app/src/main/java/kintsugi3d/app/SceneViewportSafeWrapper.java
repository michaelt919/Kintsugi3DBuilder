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

package kintsugi3d.app;

import kintsugi3d.builder.rendering.SceneViewport;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;

class SceneViewportSafeWrapper implements SceneViewport
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

    SceneViewport getSceneViewport()
    {
        return sceneViewport;
    }

    void setSceneViewport(SceneViewport sceneViewport)
    {
        this.sceneViewport = sceneViewport;
    }

    @Override
    public Object getObjectAtCoordinates(double x, double y)
    {
        return sceneViewport.getObjectAtCoordinates(x, y);
    }

    @Override
    public Vector3 get3DPositionAtCoordinates(double x, double y)
    {
        return sceneViewport.get3DPositionAtCoordinates(x, y);
    }

    @Override
    public Vector3 getViewingDirection(double x, double y)
    {
        return sceneViewport.getViewingDirection(x, y);
    }

    @Override
    public Vector3 getViewportCenter()
    {
        return sceneViewport.getViewportCenter();
    }

    @Override
    public Vector2 projectPoint(Vector3 point)
    {
        return sceneViewport.projectPoint(point);
    }

    @Override
    public float getLightWidgetScale()
    {
        return sceneViewport.getLightWidgetScale();
    }
}
