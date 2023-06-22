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

package tetzlaff.ibrelight.core;

import tetzlaff.models.ReadonlyIBRSettingsModel;

public class TextureFitSettings
{
    public final int width;
    public final int height;
    public final float gamma;

    public TextureFitSettings(int width, int height, float gamma)
    {
        if (width <= 0)
        {
            throw new IllegalArgumentException("Texture width must be greater than zero.");
        }
        else if (height <= 0)
        {
            throw new IllegalArgumentException("Texture height must be greater than zero.");
        }
        else if (gamma <= 0)
        {
            throw new IllegalArgumentException("Gamma must be greater than zero.");
        }

        this.width = width;
        this.height = height;
        this.gamma = gamma;
    }
}
