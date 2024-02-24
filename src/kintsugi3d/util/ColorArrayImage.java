package kintsugi3d.util;

/**
 * Extends ColorArrayList with explicit width and height
 */
public class ColorArrayImage extends ColorArrayList implements ColorImage
{
    private final int width;
    private final int height;

    public ColorArrayImage(float[] colorData, int width, int height)
    {
        super(colorData);
        this.width = width;
        this.height = height;
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
}
