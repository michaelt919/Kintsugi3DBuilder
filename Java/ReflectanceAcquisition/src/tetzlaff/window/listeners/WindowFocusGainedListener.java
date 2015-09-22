package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window gains focus.
 * @author Michael Tetzlaff
 *
 */
public interface WindowFocusGainedListener 
{
	/**
	 * Called when a window gains focus.
	 * @param window The window that gained focus.
	 */
	void windowFocusGained(Window window);
}
