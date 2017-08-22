package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.*;

interface Tool extends CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener, ScrollListener, KeyPressListener
{
    @Override
    default void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
    }

    @Override
    default void mouseButtonReleased(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
    }

    @Override
    default void cursorMoved(Window<?> window, double xPos, double yPos)
    {
    }

    @Override
    default void scroll(Window<?> window, double xOffset, double yOffset)
    {
    }

    @Override
    default void keyPressed(Window<?> window, int keyCode, ModifierKeys mods)
    {
    }
}
