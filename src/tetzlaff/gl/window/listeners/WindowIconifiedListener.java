package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowIconifiedListener
{
    void windowIconified(Window<?> window);
}
