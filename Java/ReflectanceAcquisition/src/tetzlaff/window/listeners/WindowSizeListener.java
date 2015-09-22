package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window is resized.
 * @author Michael Tetzlaff
 *
 */
public interface WindowSizeListener
{
	/**
	 * Called when a window is resized.
	 * @param window The window that was resized.
	 * @param width The new width of the window.
	 * @param height The new height of the window.
	 */
	void windowResized(Window window, int width, int height);
}
