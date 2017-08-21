package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface CursorPositionListener 
{
    void cursorMoved(Window<?> window, double xPos, double yPos);
}
