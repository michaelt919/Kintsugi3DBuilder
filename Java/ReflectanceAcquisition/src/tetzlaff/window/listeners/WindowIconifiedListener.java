package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window is "iconified" or "minimized."
 * @author Michael Tetzlaff
 *
 */
public interface WindowIconifiedListener 
{
	/**
	 * Called when a window is "iconified" or "minimized."
	 * @param window The window that has been iconified.
	 */
	void windowIconified(Window window);
}
