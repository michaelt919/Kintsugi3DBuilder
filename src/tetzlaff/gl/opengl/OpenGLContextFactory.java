package tetzlaff.gl.opengl;

import java.util.function.Function;

import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.glfw.ContextFactory;

public class OpenGLContextFactory implements ContextFactory<OpenGLContext>
{
    private static final OpenGLContextFactory INSTANCE = new OpenGLContextFactory();

    public static OpenGLContextFactory getInstance()
    {
        return INSTANCE;
    }

    @Override
    public OpenGLContext createContext(long glfwHandle)
    {
        OpenGLContext context = new OpenGLContext(glfwHandle);
        context.setDefaultFramebuffer(new OpenGLDefaultFramebuffer(context));
        return context;
    }

    @Override
    public OpenGLContext createContext(long glfwHandle, Function<OpenGLContext, DoubleFramebuffer<OpenGLContext>> createDefaultFramebuffer)
    {
        OpenGLContext context = new OpenGLContext(glfwHandle);
        context.setDefaultFramebuffer(createDefaultFramebuffer.apply(context));
        return context;
    }
}
