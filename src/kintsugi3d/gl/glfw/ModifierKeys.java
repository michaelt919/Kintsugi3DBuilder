/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.glfw;

import kintsugi3d.gl.window.ModifierKeysBase;

import static org.lwjgl.glfw.GLFW.*;

public class ModifierKeys extends ModifierKeysBase
{
    private final int glfwCode;

    ModifierKeys(int glfwCode)
    {
        this.glfwCode = glfwCode;
    }

    ModifierKeys(boolean shiftMod, boolean controlMod, boolean altMod, boolean superMod)
    {
        glfwCode =
            (shiftMod ? GLFW_MOD_SHIFT : 0)     |
            (controlMod ? GLFW_MOD_CONTROL : 0) |
            (altMod ? GLFW_MOD_ALT : 0)         |
            (superMod ? GLFW_MOD_SUPER : 0);
    }

    @Override
    public boolean getShiftModifier()
    {
        return (glfwCode & GLFW_MOD_SHIFT) != 0;
    }

    @Override
    public boolean getControlModifier()
    {
        return (glfwCode & GLFW_MOD_CONTROL) != 0;
    }

    @Override
    public boolean getAltModifier()
    {
        return (glfwCode & GLFW_MOD_ALT) != 0;
    }

    @Override
    public boolean getSuperModifier()
    {
        return (glfwCode & GLFW_MOD_SUPER) != 0;
    }
}
