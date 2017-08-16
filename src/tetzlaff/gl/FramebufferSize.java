package tetzlaff.gl;

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
}
