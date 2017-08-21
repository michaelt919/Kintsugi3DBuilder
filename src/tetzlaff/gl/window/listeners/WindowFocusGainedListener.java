package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowFocusGainedListener
{
    void windowFocusGained(Window<?> window);
}
