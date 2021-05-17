/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;

import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.models.ReadonlySettingsModel;

public class SpecularFitSettings extends TextureFitSettings
{
    public final int basisCount;
    public final int microfacetDistributionResolution;

    public SpecularFitSettings(int width, int height, int basisCount, int microfacetDistributionResolution, File outputDirectory, ReadonlySettingsModel additional)
    {
        super(width, height, outputDirectory, additional);
        this.basisCount = basisCount;
        this.microfacetDistributionResolution = microfacetDistributionResolution;
    }
}
