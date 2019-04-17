/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.window;

import umn.gl.core.Context;

public interface Window<ContextType extends Context<ContextType>> extends WindowListenerManager, AutoCloseable
{
    ContextType getContext();

    void show();

    void hide();

    void focus();

    boolean isHighDPI();

    boolean isWindowClosing();

    void requestWindowClose();

    void cancelWindowClose();

    @Override
    void close();

    WindowSize getWindowSize();

    WindowPosition getWindowPosition();

    void setWindowTitle(String title);

    void setWindowSize(int width, int height);

    void setWindowPosition(int x, int y);

    MouseButtonState getMouseButtonState(int buttonIndex);

    KeyState getKeyState(Key key);

    CursorPosition getCursorPosition();

    ModifierKeys getModifierKeys();

    boolean isFocused();
}
