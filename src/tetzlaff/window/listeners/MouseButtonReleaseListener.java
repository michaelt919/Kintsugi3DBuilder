package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

public interface MouseButtonReleaseListener 
{
	void mouseButtonReleased(Window window, int buttonIndex, ModifierKeys mods);
}
