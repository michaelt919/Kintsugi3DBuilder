package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowRefreshListener
{
    void windowRefreshed(Window<?> window);
}
