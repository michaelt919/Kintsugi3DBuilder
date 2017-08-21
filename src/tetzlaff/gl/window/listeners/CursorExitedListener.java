package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface CursorExitedListener
{
    void cursorExited(Window<?> window);
}
