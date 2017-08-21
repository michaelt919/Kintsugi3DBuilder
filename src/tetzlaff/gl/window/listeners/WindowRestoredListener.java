package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowRestoredListener
{
    void windowRestored(Window<?> window);
}
