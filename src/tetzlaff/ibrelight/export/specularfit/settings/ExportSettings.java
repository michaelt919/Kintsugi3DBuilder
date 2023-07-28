/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.ibrelight.export.specularfit.settings;

public class ExportSettings
{
    private boolean combineWeights = false;

    private boolean generateLowResTextures = false;
    private int minimumTextureResolution = 128;

    private boolean glTFEnabled = false;
    private boolean glTFPackTextures = false;

    public boolean isGlTFEnabled()
    {
        return glTFEnabled;
    }

    public void setGlTFEnabled(boolean glTFEnabled)
    {
        this.glTFEnabled = glTFEnabled;
    }

    public boolean isGlTFPackTextures()
    {
        return glTFPackTextures;
    }

    public void setGlTFPackTextures(boolean glTFPackTextures)
    {
        this.glTFPackTextures = glTFPackTextures;
    }

    public boolean isCombineWeights()
    {
        return combineWeights;
    }

    public void setCombineWeights(boolean combineWeights)
    {
        this.combineWeights = combineWeights;
    }

    public boolean isGenerateLowResTextures()
    {
        return generateLowResTextures;
    }

    public void setGenerateLowResTextures(boolean generateLowResTextures)
    {
        this.generateLowResTextures = generateLowResTextures;
    }

    public int getMinimumTextureResolution()
    {
        return minimumTextureResolution;
    }

    public void setMinimumTextureResolution(int minimumTextureResolution)
    {
        this.minimumTextureResolution = minimumTextureResolution;
    }
}
