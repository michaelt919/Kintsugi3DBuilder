package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when the cursor enters a window's area.
 * @author Michael Tetzlaff
 *
 */
public interface CursorEnteredListener
{
	/**
	 * Called when a cursor enters a window's area.
	 * @param window The window the cursor has entered.
	 */
	void cursorEntered(Window window);
}
