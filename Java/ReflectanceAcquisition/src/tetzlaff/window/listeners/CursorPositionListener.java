package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when the cursor moves.
 * @author Michael Tetzlaff
 *
 */
public interface CursorPositionListener 
{
	/**
	 * Called when the cursor moves.
	 * @param window The window responding to the event.
	 * @param xpos The x-coordinate of the cursor position, in screen coordinates.
	 * @param ypos The y-coordinate of the cursor position, in screen coordinates.
	 */
	void cursorMoved(Window window, double xpos, double ypos);
}
