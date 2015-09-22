package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when the cursor exits a window's area.
 * @author Michael Tetzlaff
 *
 */
public interface CursorExitedListener 
{
	/**
	 * Called when a cursor exits a window's area.
	 * @param window The window the cursor has exited.
	 */
	void cursorExited(Window window);
}
