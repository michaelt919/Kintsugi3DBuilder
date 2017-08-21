package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface FramebufferSizeListener
{
    void framebufferResized(Window<?> window, int width, int height);
}
