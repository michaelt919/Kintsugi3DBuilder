package tetzlaff.gl.glfw;

import java.util.function.Function;

import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.window.PollableWindow;
import tetzlaff.gl.window.WindowBuilderBase;

public class WindowBuilderImpl<ContextType extends WindowContextBase<ContextType>>
    extends WindowBuilderBase<ContextType>
{
    private final ContextFactory<ContextType> contextFactory;

    private Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer;

    WindowBuilderImpl(ContextFactory<ContextType> contextFactory, String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.contextFactory = contextFactory;
    }

    WindowBuilderImpl<ContextType> setDefaultFramebufferCreator(Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer)
    {
        this.createDefaultFramebuffer = createDefaultFramebuffer;
        return this;
    }

    @Override
    public PollableWindow<ContextType> create()
    {
        return new WindowImpl<>(contextFactory, createDefaultFramebuffer, this);
    }
}
