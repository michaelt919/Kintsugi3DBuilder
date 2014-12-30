package tetzlaff.window.listeners;

import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;

public interface KeyPressListener 
{
	void keyPressed(Window window, int keycode, ModifierKeys mods);
}
