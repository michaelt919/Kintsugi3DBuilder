/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import java.io.File;

public class ImageCacheSettings
{
    private File cacheParentDirectory;
    private int textureWidth;
    private int textureHeight;
    private int textureSubdiv;
    private int sampledSize;

    public File getCacheDirectory()
    {
        return new File(cacheParentDirectory, getFolderNameFromSettings());
    }

    public File getCacheParentDirectory()
    {
        return cacheParentDirectory;
    }

    public int getTextureWidth()
    {
        return textureWidth;
    }

    public int getTextureHeight()
    {
        return textureHeight;
    }

    public int getTextureSubdiv()
    {
        return textureSubdiv;
    }

    public int getSampledSize()
    {
        return sampledSize;
    }

    public void setCacheParentDirectory(File cacheParentDirectory)
    {
        this.cacheParentDirectory = cacheParentDirectory;
    }

    public void setTextureWidth(int textureWidth)
    {
        this.textureWidth = textureWidth;
    }

    public void setTextureHeight(int textureHeight)
    {
        this.textureHeight = textureHeight;
    }

    public void setTextureSubdiv(int textureSubdiv)
    {
        this.textureSubdiv = textureSubdiv;
    }

    public void setSampledSize(int sampledSize)
    {
        this.sampledSize = sampledSize;
    }

    public String getFolderNameFromSettings()
    {
        return String.format("%d-%d-%d-%d", textureWidth, textureHeight, textureSubdiv, sampledSize);
    }
}
