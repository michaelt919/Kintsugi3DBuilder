package tetzlaff.gl.core;

/**
 * A simple structure for representing the dimensions of a framebuffer.
 * @author Michael Tetzlaff
 *
 */
public class FramebufferSize
{
    /**
     * The width of the framebuffer.
     */
    public final int width;

    /**
     * The height of the framebuffer.
     */
    public final int height;

    /**
     * Creates a new FramebufferSize structure.
     * @param width The width of the framebuffer.
     * @param height The height of the framebuffer.
     */
    public FramebufferSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof FramebufferSize)
        {
            FramebufferSize other = (FramebufferSize)obj;
            return other.width == this.width && other.height == this.height;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int result = width;
        result = 31 * result + height;
        return result;
    }
}
