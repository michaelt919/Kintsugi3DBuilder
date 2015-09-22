package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window is refreshed.
 * @author Michael Tetzlaff
 *
 */
public interface WindowRefreshListener 
{
	/**
	 * Called when a window is refreshed.
	 * @param window The window that was refreshed.
	 */
	void windowRefreshed(Window window);
}
