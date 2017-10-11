package tetzlaff.ibrelight.javafx.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;

public abstract class PixelReaderBase implements PixelReader
{
    @Override
    public PixelFormat<IntBuffer> getPixelFormat()
    {
        return PixelFormat.getIntArgbInstance();
    }

    private static int clampedFixedPoint(double d)
    {
        return (int)Math.max(0, Math.min(255, Math.round(d * 255)));
    }

    @Override
    public int getArgb(int x, int y)
    {
        Color color = this.getColor(x, y);
        int a = clampedFixedPoint(color.getOpacity());
        int r = clampedFixedPoint(color.getRed());
        int g = clampedFixedPoint(color.getGreen());
        int b = clampedFixedPoint(color.getBlue());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public <T extends Buffer> void getPixels(int x, int y, int w, int h, WritablePixelFormat<T> pixelformat, T buffer, int scanlineStride)
    {
        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                pixelformat.setArgb(buffer, x + i, y + j, scanlineStride, this.getArgb(x + i, y + j));
            }
        }
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<ByteBuffer> pixelformat, byte[] buffer, int offset, int scanlineStride)
    {
        this.getPixels(x, y, w, h, pixelformat, ByteBuffer.wrap(buffer, offset, buffer.length - offset), scanlineStride);
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<IntBuffer> pixelformat, int[] buffer, int offset, int scanlineStride)
    {
        this.getPixels(x, y, w, h, pixelformat, IntBuffer.wrap(buffer, offset, buffer.length - offset), scanlineStride);
    }
}
