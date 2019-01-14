package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface MouseButtonReleaseListener
{
    void mouseButtonReleased(Window<?> window, int buttonIndex, ModifierKeys mods);
}