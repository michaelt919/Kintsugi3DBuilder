/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.window;

import tetzlaff.gl.window.listeners.*;

public interface WindowListenerManager 
{
    void addWindowPositionListener(WindowPositionListener listener);

    void addWindowSizeListener(WindowSizeListener listener);

    void addWindowCloseListener(WindowCloseListener listener);

    void addWindowRefreshListener(WindowRefreshListener listener);

    void addWindowFocusLostListener(WindowFocusLostListener listener);

    void addWindowFocusGainedListener(WindowFocusGainedListener listener);

    void addWindowIconifiedListener(WindowIconifiedListener listener);

    void addWindowRestoredListener(WindowRestoredListener listener);

    void addFramebufferSizeListener(FramebufferSizeListener listener);

    void addKeyPressListener(KeyPressListener listener);

    void addKeyReleaseListener(KeyReleaseListener listener);

    void addKeyTypeListener(KeyTypeListener listener);

    void addCharacterListener(CharacterListener listener);

    void addCharacterModifiersListener(CharacterModifiersListener listener);

    void addMouseButtonPressListener(MouseButtonPressListener listener);

    void addMouseButtonReleaseListener(MouseButtonReleaseListener listener);

    void addCursorPositionListener(CursorPositionListener listener);

    void addCursorEnteredListener(CursorEnteredListener listener);

    void addCursorExitedListener(CursorExitedListener listener);

    void addScrollListener(ScrollListener listener);
}
