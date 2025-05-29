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

package kintsugi3d.util;

import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

import java.awt.image.BufferedImage;
import java.util.AbstractList;

/**
 * Implementation of a "color list" using an AWT buffered image as the backend.
 */
public class BufferedImageColorList extends AbstractList<Vector4> implements ColorImage
{
    private final BufferedImage img;

    public BufferedImageColorList(BufferedImage img)
    {
        this.img = img;
    }

    @Override public Vector4 get(int index)
    {
        // flip vertical
        int rgb = img.getRGB(index % img.getWidth(), img.getHeight() - 1 - index / img.getWidth());

        // ARGB format
        return new Vector4(((rgb >>> 16) & 0xFF) / 255.0f, ((rgb >>> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f,
            /* alpha: */ ((rgb >>> 24) & 0xFF) / 255.0f);
    }

    @Override
    public float get(int index, int component)
    {
        switch (component)
        {
            case 0: return getRed(index);
            case 1: return getGreen(index);
            case 2: return getBlue(index);
            case 3: return getAlpha(index);
            default: throw new IllegalArgumentException("Component must be 0, 1, 2, or 3.");
        }
    }

    @Override
    public float getRed(int index)
    {
        return get(index).x;
    }

    @Override
    public float getGreen(int index)
    {
        return get(index).y;
    }

    @Override
    public float getBlue(int index)
    {
        return get(index).z;
    }

    @Override
    public float getAlpha(int index)
    {
        return get(index).w;
    }

    @Override public int size() { return img.getWidth() * img.getHeight(); }

    @Override
    public int getWidth()
    {
        return img.getWidth();
    }

    @Override
    public int getHeight()
    {
        return img.getHeight();
    }
}
