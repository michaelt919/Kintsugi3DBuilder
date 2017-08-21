package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface CursorEnteredListener
{
    void cursorEntered(Window<?> window);
}
