package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface KeyPressListener
{
    void keyPressed(Window<?> window, Key key, ModifierKeys mods);
}
