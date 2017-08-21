package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface ScrollListener
{
    void scroll(Window<?> window, double xOffset, double yOffset);
}
