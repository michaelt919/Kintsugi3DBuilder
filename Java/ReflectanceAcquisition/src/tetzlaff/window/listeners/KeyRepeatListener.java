package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

public interface KeyRepeatListener 
{
	void keyRepeated(Window window, int keycode, ModifierKeys mods);
}
