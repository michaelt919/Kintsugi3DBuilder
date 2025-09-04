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

package kintsugi3d.builder.fit.settings;

public class ExportSettings
{
    private boolean combineWeights = true;

    private boolean exportTextures = false; // by default assume that textures are being handled elsewhere
    private boolean appendModelNameToTextures = false;
    private String textureFormat = "PNG";
    private boolean generateLowResTextures = false;
    private int minimumTextureResolution = 128;

    private boolean saveModel = true;
    private boolean openViewerOnceComplete = false;

    public boolean shouldSaveModel()
    {
        return saveModel;
    }

    public void setShouldSaveModel(boolean saveModel)
    {
        this.saveModel = saveModel;
    }

    public boolean shouldSaveTextures()
    {
        return exportTextures;
    }

    public void setShouldSaveTextures(boolean exportTextures)
    {
        this.exportTextures = exportTextures;
    }

    public boolean shouldAppendModelNameToTextures()
    {
        return appendModelNameToTextures;
    }

    public void setShouldAppendModelNameToTextures(boolean appendModelNameToTextures)
    {
        this.appendModelNameToTextures = appendModelNameToTextures;
    }

    public String getTextureFormat()
    {
        return textureFormat;
    }

    public void setTextureFormat(String textureFormat)
    {
        this.textureFormat = textureFormat;
    }

    public boolean shouldCombineWeights()
    {
        return combineWeights;
    }

    public void setShouldCombineWeights(boolean combineWeights)
    {
        this.combineWeights = combineWeights;
    }

    public boolean shouldGenerateLowResTextures()
    {
        return generateLowResTextures;
    }

    public void setShouldGenerateLowResTextures(boolean generateLowResTextures)
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

    public boolean shouldOpenViewerOnceComplete()
    {
        return openViewerOnceComplete;
    }

    public void setShouldOpenViewerOnceComplete(boolean openViewerOnceComplete)
    {
        this.openViewerOnceComplete = openViewerOnceComplete;
    }
}
