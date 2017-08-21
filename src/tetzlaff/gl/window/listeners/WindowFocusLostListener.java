package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowFocusLostListener
{
    void windowFocusLost(Window<?> window);
}
