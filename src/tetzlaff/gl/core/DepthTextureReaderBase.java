/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.core;

import org.lwjgl.BufferUtils;
import tetzlaff.util.BufferedImageBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
