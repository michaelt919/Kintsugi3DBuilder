/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.state.impl;

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.state.CameraModel;

public class SimpleCameraModel implements CameraModel
{
    private Matrix4 lookMatrix;

    public SimpleCameraModel()
    {
        this(Matrix4.IDENTITY);
    }

    public SimpleCameraModel(Matrix4 lookMatrix)
    {
        this.lookMatrix = lookMatrix;
    }

    @Override
    public Matrix4 getLookMatrix()
    {
        return this.lookMatrix;
    }

    @Override
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        this.lookMatrix = lookMatrix;
    }

    @Override
    public void setTarget(Vector3 target)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        throw new UnsupportedOperationException();
    }
}
