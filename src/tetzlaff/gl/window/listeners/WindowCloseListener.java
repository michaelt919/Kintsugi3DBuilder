package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowCloseListener
{
    void windowClosing(Window<?> window);
}