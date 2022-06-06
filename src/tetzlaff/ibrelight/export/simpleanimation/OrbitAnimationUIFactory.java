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

package tetzlaff.ibrelight.export.simpleanimation;

import java.io.IOException;

import javafx.stage.Window;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.export.simpleanimation.OrbitAnimationRequest.BuilderImplementation;

public final class OrbitAnimationUIFactory
{
    private OrbitAnimationUIFactory()
    {
    }

    public static SimpleAnimationUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        SimpleAnimationUI ui = SimpleAnimationUI.create(window, modelAccess);
        ui.setBuilderSupplier(BuilderImplementation::new);
        return ui;
    }
}
