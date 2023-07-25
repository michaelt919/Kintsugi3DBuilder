/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.window;

import tetzlaff.gl.core.Context;

public interface ControllableCanvas3D<ContextType extends Context<ContextType>> extends Canvas3D<ContextType>
{
    void pressMouseButton(int buttonIndex, CursorPosition position, ModifierKeys modifierKeys);
    void releaseMouseButton(int buttonIndex, CursorPosition position, ModifierKeys modifierKeys);
    void moveCursor(CursorPosition position, ModifierKeys modifierKeys);
    void cursorEnter(CursorPosition position, ModifierKeys modifierKeys);
    void cursorExit(CursorPosition position, ModifierKeys modifierKeys);
    void scroll (double deltaX, double deltaY);
    void pressKey(Key key, ModifierKeys modifierKeys);
    void releaseKey(Key key, ModifierKeys modifierKeys);
    void typeKey(Key key, ModifierKeys modifierKeys);
    void typeCharacter(char character, ModifierKeys modifierKeys);
    void changeBounds(CanvasPosition position, CanvasSize size);
    void gainFocus();
    void loseFocus();
    void iconify();
    void restore();
    void close();
}
