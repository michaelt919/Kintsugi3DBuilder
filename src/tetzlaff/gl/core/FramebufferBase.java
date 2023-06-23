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
 * Base class for framebuffers that contains commonly used implementations for transferring native buffer data to a
 * "normal" Java array, and saving color images to file.
 * @param <ContextType>
 */
public abstract class FramebufferBase<ContextType extends Context<ContextType>> implements Framebuffer<ContextType>
{
    @Override
    public int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height)
    {
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);

        readColorBufferARGB(attachmentIndex, pixelBuffer, x, y, width, height);

        int[] pixelArray = new int[width * height];
        pixelBuffer.asIntBuffer().get(pixelArray);
        return pixelArray;
    }

    @Override
    public float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height)
    {
        FloatBuffer pixelBuffer = BufferUtils.createFloatBuffer(width * height * 4);

        readFloatingPointColorBufferRGBA(attachmentIndex, pixelBuffer, x, y, width, height);

        float[] pixelArray = new float[width * height * 4];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }

    @Override
    public int[] readIntegerColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height)
    {
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(width * height * 4);

        readIntegerColorBufferRGBA(attachmentIndex, pixelBuffer, x, y, width, height);

        int[] pixelArray = new int[width * height * 4];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }

    @Override
    public void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException
    {
        int[] pixels = this.readColorBufferARGB(attachmentIndex);
        FramebufferSize size = this.getSize();
        BufferedImage outImg = BufferedImageBuilder.build()
            .setDataFromArray(pixels, size.width, size.height)
            .flipVertical()
            .create();
        ImageIO.write(outImg, fileFormat, file);
    }

    @Override
    public void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException
    {
        int[] pixels = this.readColorBufferARGB(attachmentIndex, x, y, width, height);
        BufferedImage outImg = BufferedImageBuilder.build()
            .setDataFromArray(pixels, width, height)
            .flipVertical()
            .create();
        ImageIO.write(outImg, fileFormat, file);
    }

    public short[] readDepthBuffer(int x, int y, int width, int height)
    {
        ShortBuffer pixelBuffer = BufferUtils.createShortBuffer(width * height);

        readDepthBuffer(pixelBuffer, x, y, width, height);

        short[] pixelArray = new short[width * height];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }
}
