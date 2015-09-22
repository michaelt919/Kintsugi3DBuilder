package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window is closed.
 * @author Michael Tetzlaff
 *
 */
public interface WindowCloseListener 
{
	/**
	 * Called when a window is about to close.
	 * @param window
	 */
	void windowClosing(Window window);
}
