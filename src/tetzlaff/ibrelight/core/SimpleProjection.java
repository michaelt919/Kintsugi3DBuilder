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

package tetzlaff.ibrelight.core;

import tetzlaff.gl.vecmath.Matrix4;

/**
 * A simple perspective projection defined simply by aspect ratio and field of view.
 * @author Michael Tetzlaff
 *
 */
public class SimpleProjection implements Projection
{
    /**
     * The aspect ratio.
     */
    public final float aspectRatio;

    /**
     * The field of view.
     */
    public final float verticalFieldOfView;

    /**
     * Creates a new simple perspective projection.
     * @param aspectRatio The aspect ratio.
     * @param verticalFieldOfView The field of view.
     */
    public SimpleProjection(float aspectRatio, float verticalFieldOfView)
    {
        this.aspectRatio = aspectRatio;
        this.verticalFieldOfView = verticalFieldOfView;
    }

    @Override
    public float getVerticalFieldOfView()
    {
        return verticalFieldOfView;
    }

    @Override
    public float getAspectRatio()
    {
        return aspectRatio;
    }

    @Override
    public Matrix4 getProjectionMatrix(float nearPlane, float farPlane)
    {
        return Matrix4.perspective(this.verticalFieldOfView, this.aspectRatio, nearPlane, farPlane);
    }

    @Override
    public String toVSETString()
    {
        return String.format("f\t0\t0\t%.8f\t%.8f", aspectRatio, verticalFieldOfView);
    }
}
