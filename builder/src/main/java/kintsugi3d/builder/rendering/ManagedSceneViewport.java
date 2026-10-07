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

class ManagedSceneViewport implements SceneViewport
{
    private final RenderableManager<?> instanceManager;

    ManagedSceneViewport(RenderableManager<?> instanceManager)
    {
        this.instanceManager = instanceManager;
    }

    @Override
    public Object getObjectAtCoordinates(double x, double y)
    {
        if (instanceManager.getMainRenderable() != null)
        {
            return instanceManager.getMainRenderable().getSceneViewport().getObjectAtCoordinates(x, y);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Vector3 get3DPositionAtCoordinates(double x, double y)
    {
        if (instanceManager.getMainRenderable() != null)
        {
            return instanceManager.getMainRenderable().getSceneViewport().get3DPositionAtCoordinates(x, y);
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    @Override
    public Vector3 getViewingDirection(double x, double y)
    {
        if (instanceManager.getMainRenderable() != null)
        {
            return instanceManager.getMainRenderable().getSceneViewport().getViewingDirection(x, y);
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    @Override
    public Vector3 getViewportCenter()
    {
        if (instanceManager.getMainRenderable() != null)
        {
            return instanceManager.getMainRenderable().getSceneViewport().getViewportCenter();
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    @Override
    public Vector2 projectPoint(Vector3 point)
    {
        if (instanceManager.getMainRenderable() != null)
        {
            return instanceManager.getMainRenderable().getSceneViewport().projectPoint(point);
        }
        else
        {
            return Vector2.ZERO;
        }
    }

    @Override
    public float getLightWidgetScale()
    {
        if (instanceManager.getMainRenderable() != null)
        {
            return instanceManager.getMainRenderable().getSceneViewport().getLightWidgetScale();
        }
        else
        {
            return 1.0f;
        }
    }
}