package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window loses focus.
 * @author Michael Tetzlaff
 *
 */
public interface WindowFocusLostListener 
{
	/**
	 * Called when a window loses focus.
	 * @param window The window that lost focus.
	 */
	void windowFocusLost(Window window);
}
