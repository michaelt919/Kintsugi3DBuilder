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

package tetzlaff.gl.window;

public interface ModifierKeys
{
    ModifierKeys NONE = new ModifierKeysBase()
    {
        @Override
        public boolean getShiftModifier()
        {
            return false;
        }

        @Override
        public boolean getControlModifier()
        {
            return false;
        }

        @Override
        public boolean getAltModifier()
        {
            return false;
        }

        @Override
        public boolean getSuperModifier()
        {
            return false;
        }
    };

    boolean getShiftModifier();
    boolean getControlModifier();
    boolean getAltModifier();
    boolean getSuperModifier();
}
