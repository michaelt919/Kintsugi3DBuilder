/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */
package tetzlaff.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RadianceImageLoader loads 2D textures in high dynamic range from the Radiance (.hdr) file format.
 *
 * https://floyd.lbl.gov/radiance/refer/filefmts.pdf (page 28)
 * http://paulbourke.net/dataformats/pic/
 */

public class RadianceImageLoader
{
    private static final Pattern COMMON_RES = Pattern.compile("-Y (\\d+) \\+X (\\d+)");
    private static final Pattern ANY_RES = Pattern.compile("[-\\+]Y \\d+ [-\\+]X \\d+");

    public static class Image
    {
        public final int width;
        public final int height;
        public final float[] data;

        public Image(int width, int height, float... data)
        {
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }

    public Image read(BufferedInputStream stream) throws IOException
    {
        return this.read(stream, true, true);
    }

    public Image read(BufferedInputStream stream, boolean topToBottom, boolean leftToRight) throws IOException
    {
        stream.mark(128);
        if (!processMagicNumber(stream))
        {
            stream.reset();
            return null;
        }

        Map<String, String> vars = processVariables(stream);
        if (!"32-bit_rle_rgbe".equals(vars.get("FORMAT")))
        {
            throw new IOException("Format must be 32-bit_rle_rgbe, not: " + vars.get("FORMAT"));
        }

        int width = Integer.parseInt(vars.get("WIDTH"));
        int height = Integer.parseInt(vars.get("HEIGHT"));
        float[] data = readImage(width, height, topToBottom, true, stream);

        return new Image(width, height, data);
    }

    private float[] readImage(int width, int height, boolean topToBottom, boolean leftToRight, InputStream in)
        throws IOException
    {
        byte[] scan = new byte[width * 4];
        float[] img = new float[width * height * 3];
        for (int y = 0; y < height; y++)
        {
            // we're using OpenGL's coordinate frame where bottom is 0
            readScanLine(img, topToBottom ? height - y - 1 : y, width, leftToRight, scan, in);
        }
        return img;
    }

    private void readScanLine(float[] image, int imgY, int imgWidth, boolean leftToRight, byte[] scan,
        InputStream in) throws IOException
    {
        int baseOffset = 3 * imgY * imgWidth;

        Arrays.fill(scan, (byte) 0);
        // read the first pixel
        readAll(in, scan, 0, 4);
        boolean rle = true;
        if (imgWidth < 8 || imgWidth > 0x7fff)
        {
            rle = false; // image is too small
        }
        if (scan[0] != 2 || scan[1] != 2 || (scan[2] & 0x80) != 0)
        {
            rle = false; // not an rle scanline
        }

        if (rle)
        {
            // since we know scan[2]'s 8th bit is a 0, we don't need to mask it to properly preserve unsigned byte-ness
            // this is not the case for scan[3]
            int scanWidth = (scan[2] << 8) | (0xff & scan[3]);
            if (scanWidth != imgWidth)
            {
                throw new IOException("Wrong scanline width: " + scanWidth);
            }

            // read each channel of the RGBE data into scan
            int p = 0;
            for (int i = 0; i < 4; i++)
            {
                while (p < (i + 1) * imgWidth)
                {
                    int len = in.read();
                    if (len > 128)
                    {
                        len -= 128;
                        // run of same value (which is the next byte)
                        Arrays.fill(scan, p, p + len, (byte) in.read());
                        p += len;
                    }
                    else
                    {
                        // dump of channel
                        readAll(in, scan, p, len);
                        p += len;
                    }
                }
            }

            //            System.out.println(Arrays.toString(scan));

            // interpret the channels into pixels
            byte[] pixel = new byte[4];
            for (int x = 0; x < imgWidth; x++)
            {
                pixel[0] = scan[x];
                pixel[1] = scan[x + imgWidth];
                pixel[2] = scan[x + 2 * imgWidth];
                pixel[3] = scan[x + 3 * imgWidth];

                int xOffset = leftToRight ? 3 * x : 3 * (imgWidth - x - 1);
                convertRGBE(image, baseOffset + xOffset, pixel, 0);
            }
            //            System.out.println();
        }
        else
        {
            // scanline is flat so read it fully
            readAll(in, scan, 4);
            for (int x = 0; x < imgWidth; x++)
            {
                int xOffset = leftToRight ? 3 * x : 3 * (imgWidth - x - 1);
                convertRGBE(image, baseOffset + xOffset, scan, x * 4);
            }
        }
    }

    private void convertRGBE(float[] image, int imgOffset, byte[] rgbe, int offset)
    {
        if (rgbe[offset + 3] != 0)
        {
            // real pixel
            float v = (float) Math.pow(2.0, (0xff & rgbe[offset + 3]) - 128) / 256f;
            // these are meant to be unsigned bytes
            // FIXME rereading the text, there might need to be a + 0.5
            // inside the expression multiplied by v
            image[imgOffset] = v * (0xff & rgbe[offset]);
            image[imgOffset + 1] = v * (0xff & rgbe[offset + 1]);
            image[imgOffset + 2] = v * (0xff & rgbe[offset + 2]);
        }
        else
        {
            // black
            image[imgOffset] = 0f;
            image[imgOffset + 1] = 0f;
            image[imgOffset + 2] = 0f;
        }
    }

    private boolean processMagicNumber(InputStream in) throws IOException
    {
        // FIXME if we have fewer than this many bytes, we'll throw an exception and that breaks the whole thing
        byte[] magic = new byte[6];
        readAll(in, magic);
        if (magic[0] != '#' || magic[1] != '?')
        {
            return false;
        }

        if (magic[2] == 'R' && magic[3] == 'G' && magic[4] == 'B' && magic[5] == 'E')
        {
            return true;
        }
        // else read a few more bytes and check for RADIANCE
        if (magic[2] == 'R' && magic[3] == 'A' && magic[4] == 'D' && magic[5] == 'I')
        {
            magic = new byte[4];
            readAll(in, magic);
            if (magic[0] == 'A' && magic[1] == 'N' && magic[2] == 'C' && magic[3] == 'E')
            {
                return true;
            }
        }

        return false;
    }

    private Map<String, String> processVariables(InputStream in) throws IOException
    {
        Map<String, String> vars = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) >= 0)
        {
            if (b == '\n')
            {
                // move onto next variable
                String line = sb.toString().trim();
                if (!line.isEmpty())
                {
                    // check for variable pattern
                    int equals = line.indexOf('=');
                    if (equals >= 0)
                    {
                        vars.put(line.substring(0, equals), line.substring(equals + 1));
                    }
                    else
                    {
                        // check for resolution line
                        Matcher m = COMMON_RES.matcher(line);
                        if (m.matches())
                        {
                            vars.put("WIDTH", m.group(2));
                            vars.put("HEIGHT", m.group(1));
                            return vars;
                        }
                        else if (ANY_RES.matcher(line).matches())
                        {
                            throw new IOException("Resolution other than -Y <H> +X <W> is not supported: " +
                                line);
                        }
                        else if (!line.startsWith("#"))
                        {
                            throw new IOException("Unexpected header line: " + line);
                        }
                    }
                }
                // empty line at end of header, but we just skip it since we process the dimensions
                // as part of this method, too

                // regardless, reset buffer
                sb.setLength(0);
            }
            else
            {
                sb.append((char) b);
            }
        }
        throw new IOException("Did not encounter resolution specification");
    }

    private static void readAll(InputStream in, byte[] array) throws IOException
    {
        readAll(in, array, 0, array.length);
    }

    private static void readAll(InputStream in, byte[] array, int offset) throws IOException
    {
        readAll(in, array, offset, array.length - offset);
    }

    // read bytes from the given stream until the array has filled with length
    // fails if the end-of-stream happens before length has been read
    private static void readAll(InputStream in, byte[] array, int offset, int length) throws IOException
    {
        int currentOffset = offset;
        int remaining = length;
        int read;
        while (remaining > 0)
        {
            read = in.read(array, currentOffset, remaining);
            if (read < 0)
            {
                throw new IOException("Unexpected end of stream");
            }
            currentOffset += read;
            remaining -= read;
        }
    }
}