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

package kintsugi3d.builder.core;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.gl.core.FramebufferViewport;
import kintsugi3d.gl.vecmath.Matrix4;

/**
 * Class for representing a portion of a camera's view, i.e. for rendering a perspective scene in subdivisions.
 */
public class CameraViewport
{
    private final Matrix4 view;
    private final Matrix4 fullProjection;
    private final Matrix4 viewportCrop;
    private final Matrix4 viewportProjection;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public CameraViewport(Matrix4 view, Matrix4 fullProjection, Matrix4 viewportCrop, int x, int y, int width, int height)
    {
        this.view = view;
        this.fullProjection = fullProjection;
        this.viewportCrop = viewportCrop;
        this.viewportProjection = viewportCrop.times(fullProjection);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Copies the projection and viewport information but uses a different view matrix to construct a new CameraViewport.
     * @param newView
     * @return
     */
    public CameraViewport copyForView(Matrix4 newView)
    {
        return new CameraViewport(newView, fullProjection, viewportCrop, x, y, width, height);
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
     * The matrix which crops the full clip space into the viewport being rendered.
     * @return
     */
    public Matrix4 getViewportCrop()
    {
        return viewportCrop;
    }

    /**
     * The projection matrix for just the viewport being rendered which will only contain a portion of what the camera sees.
     * Will be equal to viewportCrop * fullProjection.
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

    public <ContextType extends Context<ContextType>> FramebufferViewport<ContextType> ofFramebuffer(Framebuffer<ContextType> framebuffer)
    {
        return framebuffer.getViewport(x, y, width, height);
    }
}
