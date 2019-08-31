/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;

public abstract class PixelReaderBase implements PixelReader
{
    @Override
    public PixelFormat<IntBuffer> getPixelFormat()
    {
        return PixelFormat.getIntArgbInstance();
    }

    private static int clampedFixedPoint(double d)
    {
        return (int)Math.max(0, Math.min(255, Math.round(d * 255)));
    }

    @Override
    public int getArgb(int x, int y)
    {
        Color color = this.getColor(x, y);
        int a = clampedFixedPoint(color.getOpacity());
        int r = clampedFixedPoint(color.getRed());
        int g = clampedFixedPoint(color.getGreen());
        int b = clampedFixedPoint(color.getBlue());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public <T extends Buffer> void getPixels(int x, int y, int w, int h, WritablePixelFormat<T> pixelformat, T buffer, int scanlineStride)
    {
        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                pixelformat.setArgb(buffer, x + i, y + j, scanlineStride, this.getArgb(x + i, y + j));
            }
        }
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<ByteBuffer> pixelformat, byte[] buffer, int offset, int scanlineStride)
    {
        this.getPixels(x, y, w, h, pixelformat, ByteBuffer.wrap(buffer, offset, buffer.length - offset), scanlineStride);
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<IntBuffer> pixelformat, int[] buffer, int offset, int scanlineStride)
    {
        this.getPixels(x, y, w, h, pixelformat, IntBuffer.wrap(buffer, offset, buffer.length - offset), scanlineStride);
    }
}
