package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

public interface KeyReleaseListener 
{
	void keyReleased(Window window, int keycode, ModifierKeys mods);
}
