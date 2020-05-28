/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.screenshot;

import java.io.IOException;

import javafx.stage.Window;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.export.screenshot.ScreenshotRequest.BuilderImplementation;

public final class ScreenshotUIFactory
{
    private ScreenshotUIFactory()
    {
    }

    public static ScreenshotUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        ScreenshotUI ui = ScreenshotUI.create(window, modelAccess);
        ui.setBuilderSupplier(BuilderImplementation::new);
        return ui;
    }
}
