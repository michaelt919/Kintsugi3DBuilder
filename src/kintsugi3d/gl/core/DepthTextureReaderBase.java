/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.core;

import org.lwjgl.BufferUtils;

import java.nio.ShortBuffer;

/**
 * Base class for texture readers that contains commonly used implementations for transferring native buffer data to a
 * "normal" Java array, and saving color images to file.
 * @param <ContextType>
 */
public abstract class DepthTextureReaderBase implements DepthTextureReader
{
    public short[] read(int x, int y, int width, int height)
    {
        ShortBuffer pixelBuffer = BufferUtils.createShortBuffer(width * height);

        read(pixelBuffer, x, y, width, height);

        short[] pixelArray = new short[width * height];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }
}
