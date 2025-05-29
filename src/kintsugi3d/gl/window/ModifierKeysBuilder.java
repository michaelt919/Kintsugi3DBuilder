/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.window;

public class ModifierKeysBuilder
{
    private boolean shiftModifier;
    private boolean controlModifier;
    private boolean altModifier;
    private boolean superModifier;

    public static ModifierKeysBuilder begin()
    {
        return new ModifierKeysBuilder();
    }

    public ModifierKeysBuilder shift()
    {
        this.shiftModifier = true;
        return this;
    }

    public ModifierKeysBuilder control()
    {
        this.controlModifier = true;
        return this;
    }

    public ModifierKeysBuilder alt()
    {
        this.altModifier = true;
        return this;
    }

    public ModifierKeysBuilder superKey()
    {
        this.superModifier = true;
        return this;
    }

    public ModifierKeys end()
    {
        return new ModifierKeysImplementation(shiftModifier, controlModifier, altModifier, superModifier);
    }

    private static final class ModifierKeysImplementation extends ModifierKeysBase
    {
        private final boolean shiftModifier;
        private final boolean controlModifier;
        private final boolean altModifier;
        private final boolean superModifier;

        private ModifierKeysImplementation(boolean shiftModifier, boolean controlModifier, boolean altModifier, boolean superModifier)
        {
            this.shiftModifier = shiftModifier;
            this.controlModifier = controlModifier;
            this.altModifier = altModifier;
            this.superModifier = superModifier;
        }

        @Override
        public boolean getShiftModifier()
        {
            return shiftModifier;
        }

        @Override
        public boolean getControlModifier()
        {
            return controlModifier;
        }

        @Override
        public boolean getAltModifier()
        {
            return altModifier;
        }

        @Override
        public boolean getSuperModifier()
        {
            return superModifier;
        }
    }
}
