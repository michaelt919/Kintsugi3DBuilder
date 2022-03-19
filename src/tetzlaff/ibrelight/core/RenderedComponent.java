package tetzlaff.ibrelight.core;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.vecmath.Matrix4;

public interface RenderedComponent<ContextType extends Context<ContextType>> extends AutoCloseable
{
    void initialize() throws Exception;
    void reloadShaders() throws Exception;

    void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 fullProjection,
              Matrix4 viewportProjection, int x, int y, int width, int height);

    default void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize size = framebuffer.getSize();
        draw(framebuffer, view, projection, projection, 0, 0, size.width, size.height);
    }
}
