package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;

interface DragTool
{
    default void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
    }

    default void mouseButtonReleased(CursorPosition cursorPosition, WindowSize windowSize)
    {
    }

    default void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
    }
}