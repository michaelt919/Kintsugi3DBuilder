package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

/**
 * A listener for when a mouse button is released.
 * @author Michael Tetzlaff
 *
 */
public interface MouseButtonReleaseListener 
{
	/**
	 * Called when a mouse button is released.
	 * @param window The window responding to the event.
	 * @param buttonIndex The index of the button that was released.
	 * 0 is generally used for the left mouse button, 1 for the right, and 2 for the middle.
	 * @param mods The modifier keys active when the button was released.
	 */
	void mouseButtonReleased(Window window, int buttonIndex, ModifierKeys mods);
}
