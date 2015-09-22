package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

/**
 * A listener for when a key is pressed.
 * @author Michael Tetzlaff
 *
 */
public interface KeyPressListener 
{
	/**
	 * Called when a key is pressed.
	 * @param window The window responding to the event.
	 * @param keycode The key that was pressed.
	 * @param mods The modifier keys active when the key was pressed.
	 */
	void keyPressed(Window window, int keycode, ModifierKeys mods);
}
