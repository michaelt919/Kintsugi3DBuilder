/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import kintsugi3d.gl.vecmath.DoubleVector4;

public class ArrayBackedImage implements AbstractImage
{
    private final int width;
    private final int height;
    private final float[] pixels;

    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public ArrayBackedImage(int width, int height, float... pixels)
    {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }

    @Override
    public DoubleVector4 getRGBA(int x, int y)
    {
        int k = (height - 1 - y) * width + x;
        return new DoubleVector4(
            Math.pow(pixels[3 * k    ], 1.0 / 2.2),
            Math.pow(pixels[3 * k + 1], 1.0 / 2.2),
            Math.pow(pixels[3 * k + 2], 1.0 / 2.2),
            1.0);
    }
}
