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

import java.io.File;
import tetzlaff.models.ReadonlySettingsModel;

public class TextureFitSettings
{
    public final int width;
    public final int height;
    public final File outputDirectory;
    public final ReadonlySettingsModel additional;

    public TextureFitSettings(int width, int height, File outputDirectory, ReadonlySettingsModel additional)
    {
        if (width <= 0)
        {
            throw new IllegalArgumentException("Texture width must be greater than zero.");
        }
        else if (height <= 0)
        {
            throw new IllegalArgumentException("Texture height must be greater than zero.");
        }
        else if (outputDirectory == null)
        {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }
        else if (additional == null)
        {
            throw new IllegalArgumentException("Additional settings cannot be null.");
        }

        this.width = width;
        this.height = height;
        this.outputDirectory = outputDirectory;
        this.additional = additional;
    }
}
