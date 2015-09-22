package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

/**
 * A listener for when a character key is typed with modifiers.
 * @author Michael Tetzlaff
 *
 */
public interface CharacterModifiersListener
{
	/**
	 * Called when a character key is typed.
	 * @param window The window responding to the event.
	 * @param c The character of the key that was typed.
	 * @param mods The modifier keys active when the key was pressed.
	 */
	void characterTypedWithModifiers(Window window, char c, ModifierKeys mods);
}
