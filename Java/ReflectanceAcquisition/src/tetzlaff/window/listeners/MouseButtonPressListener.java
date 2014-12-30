package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

public interface MouseButtonPressListener 
{
	void mouseButtonPressed(Window window, int buttonIndex, ModifierKeys mods);
}
