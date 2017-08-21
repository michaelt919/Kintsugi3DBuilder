package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface WindowPositionListener
{
    void windowMoved(Window<?> window, int xPos, int yPos);
}
