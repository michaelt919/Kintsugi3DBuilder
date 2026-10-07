/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.util;

import javafx.scene.Node;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * This class is intentionally included in kintsugi3d.builder.util rather than kintsugi3d.javafx
 * to support "plugin" JavaFX controllers without those controllers adding a dependency on the core Kintsugi JavaFX UI.
 */
public final class FXWindowHelper
{
    private FXWindowHelper()
    {
    }

    public static void requestClose(Node node)
    {
        requestClose(node.getScene().getWindow());
    }

    public static void requestClose(Window window)
    {
        if (window != null)
        {
            window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }
}
