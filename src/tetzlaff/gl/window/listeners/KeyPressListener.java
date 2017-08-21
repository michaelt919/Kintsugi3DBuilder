package tetzlaff.gl.window.listeners;

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

@FunctionalInterface
public interface KeyPressListener
{
    void keyPressed(Window<?> window, int keyCode, ModifierKeys mods);
}
