package tetzlaff.util;

import tetzlaff.gl.vecmath.DoubleVector4;

public class ArrayBackedImage implements AbstractImage
{
    private final int width;
    private final int height;
    private final float[] pixels;

    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public ArrayBackedImage(int width, int height, float... pixels)
    {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }

    @Override
    public DoubleVector4 getRGBA(int x, int y)
    {
        int k = y * width + x;
        return new DoubleVector4(
            Math.pow(pixels[3 * k    ], 1.0 / 2.2),
            Math.pow(pixels[3 * k + 1], 1.0 / 2.2),
            Math.pow(pixels[3 * k + 2], 1.0 / 2.2),
            1.0);
    }
}
