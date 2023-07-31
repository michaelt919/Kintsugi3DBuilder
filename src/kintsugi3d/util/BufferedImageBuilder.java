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

package kintsugi3d.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BufferedImageBuilder
{
    private int[] data;
    private int width;
    private int height;
    private int bufferedImageType = BufferedImage.TYPE_INT_ARGB;

    private BufferedImageBuilder()
    {
    }

    public static BufferedImageBuilder build()
    {
        return new BufferedImageBuilder();
    }

    public BufferedImageBuilder setDataFromArray(int[] data, int width, int height)
    {
        this.data = data;
        this.width = width;
        this.height = height;
        return this;
    }

    public BufferedImageBuilder loadDataFromFile(File file) throws IOException
    {
        BufferedImage img = ImageIO.read(file);
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.data = img.getRGB(0, 0, width, height, null, 0, width);
        return this;
    }

    public BufferedImageBuilder flipVertical()
    {
        for (int y = 0; y < height / 2; y++)
        {
            int limit = (y + 1) * width;
            for (int i1 = y * width, i2 = (height - y - 1) * width; i1 < limit; i1++, i2++)
            {
                int tmp = data[i1];
                data[i1] = data[i2];
                data[i2] = tmp;
            }
        }
        return this;
    }

    public BufferedImageBuilder setBufferedImageType(int bufferedImageType)
    {
        this.bufferedImageType = bufferedImageType;
        return this;
    }

    public BufferedImage create()
    {
        BufferedImage img = new BufferedImage(width, height, bufferedImageType);
        img.setRGB(0, 0, width, height, data, 0, width);
        return img;
    }
}
