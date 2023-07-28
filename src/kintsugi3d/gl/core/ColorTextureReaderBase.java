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

package kintsugi3d.gl.core;

import org.lwjgl.BufferUtils;
import kintsugi3d.util.BufferedImageBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Base class for texture readers that contains commonly used implementations for transferring native buffer data to a
 * "normal" Java array, and saving color images to file.
 */
public abstract class ColorTextureReaderBase implements ColorTextureReader
{
    @Override
    public int[] readARGB(int x, int y, int width, int height)
    {
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);

        readARGB(pixelBuffer, x, y, width, height);

        int[] pixelArray = new int[width * height];
        pixelBuffer.asIntBuffer().get(pixelArray);
        return pixelArray;
    }

    @Override
    public float[] readFloatingPointRGBA(int x, int y, int width, int height)
    {
        FloatBuffer pixelBuffer = BufferUtils.createFloatBuffer(width * height * 4);

        readFloatingPointRGBA(pixelBuffer, x, y, width, height);

        float[] pixelArray = new float[width * height * 4];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }

    @Override
    public int[] readIntegerRGBA(int x, int y, int width, int height)
    {
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(width * height * 4);

        readIntegerRGBA(pixelBuffer, x, y, width, height);

        int[] pixelArray = new int[width * height * 4];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }

    @Override
    public void saveToFile(String fileFormat, File file) throws IOException
    {
        int[] pixels = this.readARGB();
        BufferedImage outImg = BufferedImageBuilder.build()
            .setDataFromArray(pixels, getWidth(), getHeight())
            .flipVertical()
            .create();
        ImageIO.write(outImg, fileFormat, file);
    }

    @Override
    public void saveToFile(int x, int y, int width, int height, String fileFormat, File file) throws IOException
    {
        int[] pixels = this.readARGB(x, y, width, height);
        BufferedImage outImg = BufferedImageBuilder.build()
            .setDataFromArray(pixels, width, height)
            .flipVertical()
            .create();
        ImageIO.write(outImg, fileFormat, file);
    }
}
