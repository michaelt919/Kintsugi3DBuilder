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

import java.nio.FloatBuffer;
import java.util.AbstractList;

import org.lwjgl.*;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

public class ColorNativeBufferList extends AbstractList<Vector4> implements ColorList
{
    public final FloatBuffer buffer;
    private int size;

    public ColorNativeBufferList(int size)
    {
        this.buffer = BufferUtils.createFloatBuffer(size * 4);
        this.size = size;
    }

    public ColorNativeBufferList(int size, FloatBuffer buffer)
    {
        this.buffer = buffer;
        this.size = size;
    }

    @Override public Vector4 get(int index)
    {
        return new Vector4(buffer.get(4 * index), buffer.get(4 * index + 1), buffer.get(4 * index + 2), buffer.get(4 * index + 3));
    }

    public Vector3 getRGB(int index)
    {
        return new Vector3(buffer.get(4 * index), buffer.get(4 * index + 1), buffer.get(4 * index + 2));
    }

    /**
     * This is more efficient than the other version of get() when you only need to work with a single component at a time,
     * since it avoids the overhead of constructing a Vector4 object.
     * @param index
     * @param component
     * @return
     */
    @Override
    public float get(int index, int component)
    {
        return buffer.get(4 * index + component);
    }

    @Override
    public float getRed(int index)
    {
        return get(index, 0);
    }

    @Override
    public float getGreen(int index)
    {
        return get(index, 1);
    }

    @Override
    public float getBlue(int index)
    {
        return get(index, 2);
    }

    @Override
    public float getAlpha(int index)
    {
        return get(index, 3);
    }

    @Override public int size() { return size; }

    public void setSize(int size)
    {
        this.size = size;
    }
}
