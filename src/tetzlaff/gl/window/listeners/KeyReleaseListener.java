package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

public interface KeyReleaseListener 
{
    void keyReleased(Window<?> window, int keycode, ModifierKeys mods);
}
