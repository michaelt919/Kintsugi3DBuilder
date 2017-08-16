package tetzlaff.gl.glfw;

import tetzlaff.gl.window.WindowBuilder;
import tetzlaff.gl.window.WindowBuilderBase;

public class GLFWWindowBuilder<ContextType extends GLFWWindowContextBase<ContextType>>
    extends WindowBuilderBase<GLFWWindow<ContextType>>
    implements WindowBuilder<GLFWWindow<ContextType>>
{
    private GLFWContextFactory<ContextType> contextFactory;

    GLFWWindowBuilder(GLFWContextFactory<ContextType> contextFactory, String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.contextFactory = contextFactory;
    }

    @Override
    public GLFWWindow<ContextType> create()
    {
        return new GLFWWindow<ContextType>(contextFactory, this.getWidth(), this.getHeight(), this.getTitle(),
                this.getX(), this.getY(), this.isResizable(), this.getMultisamples());
    }
}
