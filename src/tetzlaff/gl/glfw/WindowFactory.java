package tetzlaff.gl.glfw;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLContextFactory;

public final class WindowFactory
{
    private WindowFactory()
    {
    }

    public static WindowBuilderImpl<OpenGLContext> buildOpenGLWindow(String title, int width, int height)
    {
        return new WindowBuilderImpl<>(OpenGLContextFactory.getInstance(), title, width, height);
    }
}
