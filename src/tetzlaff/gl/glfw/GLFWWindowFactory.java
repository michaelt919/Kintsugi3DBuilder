package tetzlaff.gl.glfw;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLContextFactory;

public final class GLFWWindowFactory 
{
    private GLFWWindowFactory()
    {
    }

    public static GLFWWindowBuilder<OpenGLContext> buildOpenGLWindow(String title, int width, int height)
    {
        return new GLFWWindowBuilder<>(OpenGLContextFactory.getInstance(), title, width, height);
    }
}
