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

package kintsugi3d.gl.util;

import kintsugi3d.gl.vecmath.IntVector2;
import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.ColorTransform;
import sun.java2d.cmm.PCMM;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class ImageHelper
{
    public static final String ERROR_UNSUPPORTED_IMAGE_FORMAT = "Error: Unsupported image format.";

    /**
     * Whether or not to use (transform to sRGB) or ignore (reinterpret as sRGB) any ICC transformation
     * specified in images loaded from file or input stream
     */
    private static final boolean ICC_TRANSFORM_ENABLED = false;

    private final BufferedImage image;

    public static IntVector2 dimensionsOf(File file) throws IOException
    {
        InputStream input = new FileInputStream(file);
        try (ImageInputStream iis = ImageIO.createImageInputStream(input))
        {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext())
            {
                ImageReader reader = readers.next();
                try
                {
                    reader.setInput(iis);
                    return new IntVector2(reader.getWidth(0), reader.getHeight(0));
                }
                finally
                {
                    reader.dispose();
                }
            }
        }

        throw unsupportedFormatException(file);
    }

    public static ImageHelper read(File file) throws IOException
    {
        ImageHelper raw = new ImageHelper(ImageIO.read(file));
        raw.validate(file);
        return ICC_TRANSFORM_ENABLED ? raw.convertedICCToSRGB() : raw.forcedSRGB();
    }

    public static ImageHelper read(InputStream input) throws IOException
    {
        ImageHelper raw = new ImageHelper(ImageIO.read(input));
        raw.validate(null);
        return ICC_TRANSFORM_ENABLED ? raw.convertedICCToSRGB() : raw.forcedSRGB();
    }

    public static ImageHelper of(BufferedImage image)
    {
        ImageHelper raw = new ImageHelper(image);
        Objects.requireNonNull(image);
        return ICC_TRANSFORM_ENABLED ? raw.convertedICCToSRGB() : raw.forcedSRGB();
    }

    private ImageHelper validate(File file) throws IOException
    {
        if (image == null)
        {
            throw unsupportedFormatException(file);
        }

        return this;
    }

    private static IOException unsupportedFormatException(File file)
    {
        return new IOException(file == null ? ERROR_UNSUPPORTED_IMAGE_FORMAT
            : String.format("%s (%s)", ERROR_UNSUPPORTED_IMAGE_FORMAT, file.getName()));
    }

    private ImageHelper(BufferedImage image)
    {
        this.image = image;
    }

    public BufferedImage getBufferedImage()
    {
        return image;
    }

    private ImageHelper withAlphaMask(ImageHelper mask)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage source = mask.getBufferedImage();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width);
        int[] maskPixels = source.getRGB(0, 0, width, height, null, 0, width);

        int[] resultPixels = result.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < imagePixels.length; i++)
        {
            int color = imagePixels[i] & 0x00ffffff; // Mask preexisting alpha
            int alpha = maskPixels[i] << 24; // Shift blue to alpha
            resultPixels[i] = color | alpha;
        }

        result.setRGB(0, 0, width, height, resultPixels, 0, width);
        return new ImageHelper(result);
    }

    public ImageHelper withAlphaMask(BufferedImage mask)
    {
        if (mask != null)
        {
            return withAlphaMask(ImageHelper.of(mask)
                .scaledToResolution(image.getWidth(), image.getHeight())); // scale if necessary (skipped if same resolution)
        }
        else
        {
            return this;
        }
    }

    public ImageHelper withAlphaMask(InputStream maskStream) throws IOException
    {
        if (maskStream != null)
        {
            return withAlphaMask(ImageHelper.read(maskStream))
                .scaledToResolution(image.getWidth(), image.getHeight()); // scale if necessary (skipped if same resolution)
        }
        else
        {
            return this;
        }
    }

    public ImageHelper withAlphaMask(File maskFile) throws IOException
    {
        if (maskFile != null && maskFile.exists())
        {
            return withAlphaMask(ImageHelper.read(maskFile))
                .scaledToResolution(image.getWidth(), image.getHeight()); // scale if necessary (skipped if same resolution
        }
        else
        {
            return this;
        }
    }

    public ImageHelper scaledBy(double factorX, double factorY)
    {
        if (factorX == 1.0 && factorY == 1.0)
        {
            return this;
        }
        else
        {
            return scaledToResolution((int)Math.round(image.getWidth() * factorX), (int)Math.round(image.getHeight() * factorY));
        }
    }

    public ImageHelper scaledBy(double factor)
    {
        return scaledBy(factor, factor);
    }

    public ImageHelper scaledToResolution(int width, int height)
    {
        if (width == image.getWidth() && height == image.getHeight())
        {
            return this;
        }
        else
        {
            // Just use java.awt graphics for simple scaling
            // drawImage is more lightweight and possibly higher quality than AffineTransform.
            BufferedImage resized = new BufferedImage(width, height, image.getType());
            Graphics resizedGraphics = resized.createGraphics();
            resizedGraphics.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            resizedGraphics.dispose();
            return new ImageHelper(resized);
        }
    }

    private ImageHelper forcedSRGB()
    {
        // TODO is there a cleaner way to ignore input color space?  Maybe work with the Raster object directly?
        return new ImageHelper(
            image == null || !(image.getColorModel() instanceof ComponentColorModel) || image.getColorModel().getNumComponents() < 3
                // skip forced color space if not ComponentColorModel or if the number of components is < 3 (i.e. grayscale)
                ? image
                : new BufferedImage(
                new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    image.getColorModel().hasAlpha(),
                    image.isAlphaPremultiplied(),
                    image.getTransparency(),
                    image.getColorModel().getTransferType()),
                image.getRaster(),
                image.isAlphaPremultiplied(),
                null));
    }

    private ImageHelper convertedICCToSRGB()
    {
        if (image == null || !(image.getColorModel().getColorSpace() instanceof ICC_ColorSpace))
        {
            return this;
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
                return this;
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

                BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(),
                    image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                this2srgb.colorConvert(image.getRaster(), result.getRaster(), minVal, maxVal, destMinVal, destMaxVal);
                return new ImageHelper(result);
            }
        }
    }

    public ImageHelper saveAtResolution(String format, File scaledFile, int width, int height) throws IOException
    {
        BufferedImage scaled = scaledToResolution(width, height).image;
        ImageIO.write(scaled, format, scaledFile);
        return this;
    }

    public ImageHelper saveAtScale(String format, File file, double factor) throws IOException
    {
        saveAtResolution(format, file, (int)(image.getWidth() * factor), (int)(image.getHeight() * factor));
        return this;
    }

    public ImageHelper saveAtResolution(String format, File file, int height) throws IOException
    {
        saveAtScale(format, file, (double) height / image.getHeight());
        return this;
    }
}
