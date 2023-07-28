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

package kintsugi3d.gl.window.listeners;

import kintsugi3d.gl.window.Canvas3D;
import kintsugi3d.gl.window.ModifierKeys;

@FunctionalInterface
public interface MouseButtonPressListener
{
    void mouseButtonPressed(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, int buttonIndex, ModifierKeys mods);
}
