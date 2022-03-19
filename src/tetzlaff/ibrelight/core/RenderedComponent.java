package tetzlaff.ibrelight.core;

import java.io.FileNotFoundException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.vecmath.Matrix4;

public interface RenderedComponent<ContextType extends Context<ContextType>> extends AutoCloseable
{
    void initialize() throws Exception;

    /**
     * May reload shaders if compiled settings have changed
     * @throws Exception
     */
    default void update() throws Exception
    {
    }

    /**
     * Force reload shaders
     * @throws Exception
     */
    void reloadShaders() throws Exception;

    void draw(Framebuffer<ContextType> framebuffer, CameraViewport cameraViewport);

    default void draw(Framebuffer<ContextType> framebuffer, boolean expectingModel, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize size = framebuffer.getSize();
        draw(framebuffer, new CameraViewport(expectingModel, view, projection, projection, 0, 0, size.width, size.height));
    }
}
