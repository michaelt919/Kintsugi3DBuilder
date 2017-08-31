package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface KeyReleaseListener 
{
    void keyReleased(Window<?> window, Key key, ModifierKeys mods);
}
