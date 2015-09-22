package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

/**
 * A listener for when a key is repeated.
 * @author Michael Tetzlaff
 *
 */
public interface KeyRepeatListener 
{
	/**
	 * Called when a key is repeated.
	 * @param window The window responding to the event.
	 * @param keycode The key that was repeated.
	 * @param mods The modifier keys active when the key was repeated.
	 */
	void keyRepeated(Window window, int keycode, ModifierKeys mods);
}
