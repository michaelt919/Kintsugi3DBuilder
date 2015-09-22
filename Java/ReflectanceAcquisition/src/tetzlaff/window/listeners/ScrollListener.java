package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when the scroll wheel is turned.
 * @author Michael Tetzlaff
 *
 */
public interface ScrollListener 
{
	/**
	 * Called when the scroll wheel is turned.
	 * @param window The window responding to the event.
	 * @param xoffset The amount and direction of horizontal scrolling.
	 * @param yoffset The amount and direction of vertical scrolling.
	 */
	void scroll(Window window, double xoffset, double yoffset);
}
