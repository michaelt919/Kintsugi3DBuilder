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

package kintsugi3d.builder.resources.project;

import java.io.File;

public class ImageCacheSettings
{
    private File cacheParentDirectory;
    private int textureWidth;
    private int textureHeight;
    private int textureSubdiv;
    private int sampledSize;
    private String cacheFolderName;

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
        if (cacheFolderName != null)
        {
            return String.format("%s/%d-%d-%d-%d", cacheFolderName, textureWidth, textureHeight, textureSubdiv, sampledSize);
        }
        else
        {
            return String.format("%d-%d-%d-%d", textureWidth, textureHeight, textureSubdiv, sampledSize);
        }
    }

    public File getBlockDir(int i, int j)
    {
        return new File(getCacheDirectory(), String.format("%d_%d", i, j));
    }

    public int getBlockStartX(int i)
    {
        return (int) Math.round((double) i * (double) textureWidth / (double) textureSubdiv);
    }

    public int getBlockStartY(int j)
    {
        return (int) Math.round((double) j * (double) textureHeight / (double) textureSubdiv);
    }

    public String getCacheFolderName()
    {
        return cacheFolderName;
    }

    public void setCacheFolderName(String cacheFolderName)
    {
        this.cacheFolderName = cacheFolderName;
    }
}
