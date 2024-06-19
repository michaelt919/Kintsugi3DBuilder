/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import java.util.Objects;

import kintsugi3d.gl.window.Key;
import kintsugi3d.gl.window.ModifierKeys;

public class KeyPress
{
    private final Key key;
    private final ModifierKeys modifierKeys;

    public KeyPress(Key key, ModifierKeys modifierKeys)
    {
        this.key = key;
        this.modifierKeys = modifierKeys;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof KeyPress)
        {
            KeyPress otherMapping = (KeyPress)obj;
            return otherMapping.key == this.key && Objects.equals(otherMapping.modifierKeys, this.modifierKeys);
        }
        else
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(40);

        if (modifierKeys.getShiftModifier())
        {
            builder.append("SHIFT-");
        }

        if (modifierKeys.getSuperModifier())
        {
            builder.append("SUPER-");
        }

        if (modifierKeys.getControlModifier())
        {
            builder.append("CTRL-");
        }

        if (modifierKeys.getAltModifier())
        {
            builder.append("ALT-");
        }

        builder.append(key);

        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        int result = key.hashCode();
        result = 31 * result + modifierKeys.hashCode();
        return result;
    }
}
