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

package kintsugi3d.builder.core;

import kintsugi3d.gl.core.IntLike;

public enum ColorAppearanceMode implements IntLike
{
    ANALYTIC(0),
    IMAGE_SPACE(1),
    TEXTURE_SPACE(2),
    TEXTURE_SPACE_CROP(3);

    private final int intValue;

    ColorAppearanceMode(int intValue)
    {
        this.intValue = intValue;
    }

    /**
     * For uniforms and vertex attributes
     * @return the integer encoding of this instance
     */
    @Override
    public int getIntValue()
    {
        return intValue;
    }

    /**
     * For preprocessor defines
     * @return the String form of the integer encoding of this instance
     */
    public String toString()
    {
        return Integer.toString(intValue);
    }
}
