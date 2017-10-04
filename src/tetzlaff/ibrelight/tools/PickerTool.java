package tetzlaff.ibrelight.tools;

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;

public interface PickerTool
{
    boolean mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods);
    boolean mouseButtonReleased(Window<?> window, int buttonIndex, ModifierKeys mods);
    boolean cursorMoved(Window<?> window, double xPos, double yPos);
}
