/*
 *  Copyright (c) Michael Tetzlaff 2022
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import tetzlaff.gl.vecmath.Matrix4;

/**
 * Class for representing a portion of a camera's view, i.e. for rendering a perspective scene in subdivisions.
 */
public class CameraViewport
{
    private final boolean expectingModelTransform;
    private final Matrix4 view;
    private final Matrix4 fullProjection;
    private final Matrix4 viewportProjection;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public CameraViewport(boolean expectingModel, Matrix4 view, Matrix4 fullProjection, Matrix4 viewportProjection, int x, int y, int width, int height)
    {
        this.expectingModelTransform = expectingModel;
        this.view = view;
        this.fullProjection = fullProjection;
        this.viewportProjection = viewportProjection;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * If true, the view matrix is expected to be applied after the model transformation specified by the user in the IBRelight UI.
     * If false, the view matrix should be applied directly to the object's local coordinate space.
     * @return
     */
    public boolean isExpectingModelTransform()
    {
        return expectingModelTransform;
    }

    public Matrix4 getView()
    {
        return view;
    }

    /**
     * The full projection matrix that would fill the entire window
     * @return
     */
    public Matrix4 getFullProjection()
    {
        return fullProjection;
    }

    /**
     * The projection matrix for just the viewport being rendered which will only contain a portion of what the camera sees.
     * @return
     */
    public Matrix4 getViewportProjection()
    {
        return viewportProjection;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}
