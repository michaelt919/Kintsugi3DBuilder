package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowSizeListener
{
    void windowResized(Window<?> window, int width, int height);
}
