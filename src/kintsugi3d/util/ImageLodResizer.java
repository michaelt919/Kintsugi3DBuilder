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

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageLodResizer
{

    private static final int DEFAULT_MIN_SIZE = 128;
    private final File outputDirectory;
    private final BufferedImage inputImage;

    public ImageLodResizer(File inputFile) throws IOException
    {
        outputDirectory = inputFile.getParentFile();
        inputImage = ImageIO.read(inputFile);
    }

    public ImageLodResizer(BufferedImage inputImage, File outputDirectory)
    {
        this.inputImage = inputImage;
        this.outputDirectory = outputDirectory;
    }

    public static void generateLods(File inputFile) throws IOException
    {
        generateLods(inputFile, DEFAULT_MIN_SIZE);
    }

    public static void generateLods(File inputFile, int minSize) throws IOException
    {
        File dir = inputFile.getParentFile();
        ImageLodResizer resize = new ImageLodResizer(inputFile);

        String filename = inputFile.getName();
        String extension = "";
        int i = filename.lastIndexOf('.'); //Strip file extension
        if (i > 0)
        {
            extension = filename.substring(i);
            filename = filename.substring(0, i);
        }

        for (int size = resize.inputImage.getHeight() / 2; size >= minSize; size /= 2)
        {
            StringBuilder sb = new StringBuilder(filename);
            sb.append("-");
            sb.append(size);
            sb.append(extension);

            resize.saveAtResolution(new File(dir, sb.toString()), size);
        }
    }

    public void saveAtResolution(File file, int height) throws IOException
    {
        saveAtScale(file, (double) height / inputImage.getHeight());
    }

    public void saveAtScale(File file, double factor) throws IOException
    {
        saveAtResolution(file, (int)(inputImage.getWidth() * factor), (int)(inputImage.getHeight() * factor));
    }

    public void saveAtResolution(File file, int width, int height) throws IOException
    {
        BufferedImage scaled = scaleToResolution(width, height);
        ImageIO.write(scaled, getFormatFor(file), file);
    }

    private static String getFormatFor(File file)
    {
        int i = file.getName().lastIndexOf('.');
        return file.getName().substring(i + 1).toUpperCase();
    }

    public BufferedImage scaleToResolution(int width, int height)
    {
        return scaleBy((double) width / inputImage.getWidth(), (double) height / inputImage.getHeight());
    }

    private BufferedImage scaleBy(double factor)
    {
        return scaleBy(factor, factor);
    }

    private BufferedImage scaleBy(double factorX, double factorY)
    {
        int w = (int) (factorX * inputImage.getWidth());
        int h = (int) (factorY * inputImage.getHeight());
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform transform = new AffineTransform();
        transform.scale(factorX, factorY);
        AffineTransformOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        scaled = operation.filter(inputImage, scaled);
        return scaled;
    }

}
