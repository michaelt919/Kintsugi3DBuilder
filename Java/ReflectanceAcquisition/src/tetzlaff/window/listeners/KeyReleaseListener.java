package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

/**
 * A listener for when a key is released.
 * @author Michael Tetzlaff
 *
 */
public interface KeyReleaseListener 
{
	/**
	 * Called when a key is released.
	 * @param window The window responding to the event.
	 * @param keycode The key that was released.
	 * @param mods The modifier keys active when the key was released.
	 */
	void keyReleased(Window window, int keycode, ModifierKeys mods);
}
