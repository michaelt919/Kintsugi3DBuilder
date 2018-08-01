package tetzlaff.gl.window;

import tetzlaff.gl.core.Context;

public interface WindowBuilder<ContextType extends Context<ContextType>> extends WindowSpecification
{
    WindowBuilder<ContextType> setX(int x);
    WindowBuilder<ContextType> setY(int y);
    WindowBuilder<ContextType> setResizable(boolean resizable);
    WindowBuilder<ContextType> setMultisamples(int multisamples);

    PollableWindow<ContextType> create();
}
