package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

public interface ScrollListener 
{
	void scroll(Window<?> window, double xoffset, double yoffset);
}
