package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Window;

public interface WindowSizeListener
{
	void windowResized(Window<?> window, int width, int height);
}
