package tetzlaff.gl.glfw;

import tetzlaff.gl.window.PollableWindow;
import tetzlaff.gl.window.WindowBuilderBase;

public class WindowBuilderImpl<ContextType extends WindowContextBase<ContextType>>
    extends WindowBuilderBase<ContextType>
{
    private final ContextFactory<ContextType> contextFactory;

    WindowBuilderImpl(ContextFactory<ContextType> contextFactory, String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.contextFactory = contextFactory;
    }

    @Override
    public PollableWindow<ContextType> create()
    {
        return new WindowImpl<>(contextFactory, this);
    }
}
