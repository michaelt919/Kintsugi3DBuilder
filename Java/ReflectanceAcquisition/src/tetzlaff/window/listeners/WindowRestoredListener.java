package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a window is restored from an iconified state.
 * @author Michael Tetzlaff
 *
 */
public interface WindowRestoredListener 
{
	/**
	 * Called when a window is restored from an iconified state.
	 * @param window The window that has been restored.
	 */
	void windowRestored(Window window);
}
