/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.screenshot;

import java.io.IOException;

import javafx.stage.Window;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.export.screenshot.ScreenshotRequest.BuilderImplementation;

public final class ScreenshotUIFactory
{
    private ScreenshotUIFactory()
    {
    }

    public static ScreenshotUI create(Window window, Kintsugi3DBuilderState modelAccess) throws IOException
    {
        ScreenshotUI ui = ScreenshotUI.create(window, modelAccess);
        ui.setBuilderSupplier(BuilderImplementation::new);
        return ui;
    }
}
