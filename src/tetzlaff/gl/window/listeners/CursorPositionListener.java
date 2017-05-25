package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

public interface CursorPositionListener 
{
	void cursorMoved(Window<?> window, double xpos, double ypos);
}
