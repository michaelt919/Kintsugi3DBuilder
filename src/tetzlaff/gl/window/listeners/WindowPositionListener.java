package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

public interface WindowPositionListener 
{
    void windowMoved(Window<?> window, int xpos, int ypos);
}
