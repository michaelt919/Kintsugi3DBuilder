package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface MouseButtonPressListener
{
    void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods);
}
