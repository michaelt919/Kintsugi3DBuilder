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

import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.ColorTransform;
import sun.java2d.cmm.PCMM;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class ImageHelper
{
    private static final int DEFAULT_MIN_SIZE = 128;
    private BufferedImage image;

    public ImageHelper(File imageFile) throws IOException
    {
        image = ImageIO.read(imageFile);
    }

    public ImageHelper(BufferedImage image)
    {
        this.image = image;
    }

    public static void generateLods(File inputFile) throws IOException
    {
        generateLods(inputFile, DEFAULT_MIN_SIZE);
    }

    public static void generateLods(File inputFile, int minSize) throws IOException
    {
        File dir = inputFile.getParentFile();
        ImageHelper resize = new ImageHelper(inputFile);

        String filename = inputFile.getName();
        String extension = "";
        int i = filename.lastIndexOf('.'); //Strip file extension
        if (i > 0)
        {
            extension = filename.substring(i);
            filename = filename.substring(0, i);
        }

        for (int size = resize.image.getHeight() / 2; size >= minSize; size /= 2)
        {
            resize.saveAtResolution(new File(dir, filename + "-" + size + extension), size);
        }
    }

    public ImageHelper setAlphaMask(BufferedImage mask)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width);
        int[] maskPixels = mask.getRGB(0, 0, width, height, null, 0, width);

        int[] resultPixels = result.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < imagePixels.length; i++)
        {
            int color = imagePixels[i] & 0x00ffffff; // Mask preexisting alpha
            int alpha = maskPixels[i] << 24; // Shift blue to alpha
            resultPixels[i] = color | alpha;
        }

        result.setRGB(0, 0, width, height, resultPixels, 0, width);
        image = result;

        return this;
    }

    public ImageHelper setAlphaMask(File maskFile) throws IOException
    {
        return setAlphaMask(ImageIO.read(maskFile));
    }

    public void saveAtResolution(File file, int height) throws IOException
    {
        saveAtScale(file, (double) height / image.getHeight());
    }

    public void saveAtScale(File file, double factor) throws IOException
    {
        saveAtResolution(file, (int)(image.getWidth() * factor), (int)(image.getHeight() * factor));
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

    public BufferedImage getBufferedImage()
    {
        return image;
    }

    public BufferedImage scaleToResolution(int width, int height)
    {
        return scaleBy((double) width / image.getWidth(), (double) height / image.getHeight());
    }

    public BufferedImage scaleBy(double factor)
    {
        return scaleBy(factor, factor);
    }

    public BufferedImage scaleBy(double factorX, double factorY)
    {
        int w = (int) (factorX * image.getWidth());
        int h = (int) (factorY * image.getHeight());
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform transform = new AffineTransform();
        transform.scale(factorX, factorY);
        AffineTransformOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        scaled = operation.filter(image, scaled);
        return scaled;
    }

    public BufferedImage forceSRGB ()
    {
        // TODO is there a cleaner way to ignore input color space?  Maybe work with the Raster object directly?
        return image == null || !(image.getColorModel() instanceof ComponentColorModel) ? image
            : new BufferedImage(
            new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                image.getColorModel().hasAlpha(),
                image.isAlphaPremultiplied(),
                image.getTransparency(),
                image.getColorModel().getTransferType()),
            image.getRaster(),
            image.isAlphaPremultiplied(),
            null);
    }

    public BufferedImage convertICCToSRGB()
    {
        if (image == null || !(image.getColorModel().getColorSpace() instanceof ICC_ColorSpace))
        {
            return image;
        }
        else
        {
            // Copied from ICC_ColorSpace::toRGB
            ColorTransform[] transformList = new ColorTransform[2];
            ICC_ColorSpace srgbCS = (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
            PCMM mdl = CMSManager.getModule();
            ICC_ColorSpace colorSpace = (ICC_ColorSpace) image.getColorModel().getColorSpace();

            if (Objects.equals(srgbCS, colorSpace))
            {
                // Color spaces are the same; no conversion necessary
                // (and attempting to convert seems to cause a segfault)
                return image;
            }
            else
            {
                transformList[0] = mdl.createTransform(
                    colorSpace.getProfile(), ColorTransform.Any, ColorTransform.In);
                transformList[1] = mdl.createTransform(
                    srgbCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
                ColorTransform this2srgb = mdl.createTransform(transformList);

                // Set component scaling
                int nc = colorSpace.getNumComponents();
                float[] minVal = new float[nc];
                float[] maxVal = new float[nc];

                for (int i = 0; i < nc; i++)
                {
                    minVal[i] = colorSpace.getMinValue(i); // in case getMinVal is overridden
                    maxVal[i] = colorSpace.getMaxValue(i); // in case getMaxVal is overridden
                }

                float[] destMinVal = new float[nc];
                Arrays.fill(destMinVal, 0.0f);

                float[] destMaxVal = new float[nc];
                Arrays.fill(destMaxVal, 1.0f);

                BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                this2srgb.colorConvert(image.getRaster(), result.getRaster(), minVal, maxVal, destMinVal, destMaxVal);
                return result;
            }
        }
    }
}
