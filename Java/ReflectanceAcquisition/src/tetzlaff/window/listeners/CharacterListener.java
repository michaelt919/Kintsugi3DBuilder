package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a character key is typed.
 * @author Michael Tetzlaff
 *
 */
public interface CharacterListener 
{
	/**
	 * Called when a character key is typed.
	 * @param window The window responding to the event.
	 * @param c The character of the key that was typed.
	 */
	void characterTyped(Window window, char c);
}
