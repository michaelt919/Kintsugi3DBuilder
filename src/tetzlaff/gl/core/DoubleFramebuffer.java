package tetzlaff.gl.core;

public interface DoubleFramebuffer<ContextType extends Context<ContextType>> extends Framebuffer<ContextType>
{
    void swapBuffers();
}
