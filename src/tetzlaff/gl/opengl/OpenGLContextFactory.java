package tetzlaff.gl.opengl;

import tetzlaff.gl.glfw.GLFWContextFactory;

public class OpenGLContextFactory implements GLFWContextFactory<OpenGLContext>
{
    private static final OpenGLContextFactory INSTANCE = new OpenGLContextFactory();

    public static OpenGLContextFactory getInstance()
    {
        return INSTANCE;
    }

    @Override
    public OpenGLContext createContext(long glfwHandle)
    {
        return new OpenGLContext(glfwHandle);
    }
}
