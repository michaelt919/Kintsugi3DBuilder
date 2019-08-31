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

public abstract class ModifierKeysBase implements ModifierKeys
{
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ModifierKeys)
        {
            ModifierKeys modifierKeys = (ModifierKeys) obj;
            return (modifierKeys.getShiftModifier()     || !this.getShiftModifier())
                && (modifierKeys.getControlModifier()   || !this.getControlModifier())
                && (modifierKeys.getAltModifier()       || !this.getAltModifier())
                && (modifierKeys.getSuperModifier()     || !this.getSuperModifier());
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return  (this.getShiftModifier()   ? 1 : 0) |
                (this.getControlModifier() ? 2 : 0) |
                (this.getAltModifier()     ? 4 : 0) |
                (this.getSuperModifier()   ? 8 : 0);
    }
}
