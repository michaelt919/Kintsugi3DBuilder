package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * An listener for when a window is moved.
 * @author Michael Tetzlaff
 *
 */
public interface WindowPositionListener 
{
	/**
	 * Called when a window is moved.
	 * @param window The window that was moved.
	 * @param xpos The x-coordinate of the left edge of the window, in logical pixels.
	 * @param ypos The y-coordinate of the left edge of the window, in logical pixels.
	 */
	void windowMoved(Window window, int xpos, int ypos);
}
