package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

public interface CharacterModifiersListener
{
	void characterTypedWithModifiers(Window window, char c, ModifierKeys mods);
}
