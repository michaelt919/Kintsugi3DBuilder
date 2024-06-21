/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;

public class SimpleLoadOptionsModel implements LoadOptionsModel
{
    private boolean colorImagesRequested = true;
    private boolean mipmapsRequested = true;
    private boolean compressionRequested = true;
    private boolean alphaRequested = true;
    private boolean depthImagesRequested = true;
    private int depthImageWidth = 512;
    private int depthImageHeight = 512;
    private int previewImageWidth = 1024;
    private int previewImageHeight = 1024;
    private int maxLoadingThreads = 12;

    @Override
    public boolean areColorImagesRequested()
    {
        return this.colorImagesRequested;
    }

    @Override
    public int getPreviewImageWidth()
    {
        return previewImageWidth;
    }

    @Override
    public void setPreviewImageWidth(int previewImageWidth)
    {
        this.previewImageWidth = previewImageWidth;
    }

    @Override
    public int getPreviewImageHeight()
    {
        return previewImageHeight;
    }

    @Override
    public void setColorImagesRequested(boolean colorImagesRequested)
    {
        this.colorImagesRequested = colorImagesRequested;
    }

    @Override
    public void setMipmapsRequested(boolean mipmapsRequested)
    {
        this.mipmapsRequested = mipmapsRequested;
    }

    @Override
    public void setCompressionRequested(boolean compressionRequested)
    {
        this.compressionRequested = compressionRequested;
    }

    @Override
    public void setDepthImagesRequested(boolean depthImagesRequested)
    {
        this.depthImagesRequested = depthImagesRequested;
    }

    @Override
    public void setDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth = depthImageWidth;
    }

    @Override
    public void setDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight = depthImageHeight;
    }

    @Override
    public void setPreviewImageHeight(int previewImageHeight)
    {
        this.previewImageHeight = previewImageHeight;
    }

    public SimpleLoadOptionsModel requestColorImages(boolean colorImagesRequested)
    {
        this.colorImagesRequested = colorImagesRequested;
        return this;
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return this.mipmapsRequested;
    }

    public SimpleLoadOptionsModel requstMipmaps(boolean mipmapsRequested)
    {
        this.mipmapsRequested = mipmapsRequested;
        return this;
    }

    @Override
    public boolean isCompressionRequested()
    {
        return this.compressionRequested;
    }

    @Override
    public boolean isAlphaRequested()
    {
        return this.alphaRequested;
    }

    public SimpleLoadOptionsModel requestAlpha(boolean alphaRequested)
    {
        this.alphaRequested = alphaRequested;
        return this;
    }

    public SimpleLoadOptionsModel requestCompression(boolean compressionRequested)
    {
        this.compressionRequested = compressionRequested;
        return this;
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return this.depthImagesRequested;
    }

    public SimpleLoadOptionsModel requestDepthImages(boolean depthImagesRequested)
    {
        this.depthImagesRequested = depthImagesRequested;
        return this;
    }

    @Override
    public int getDepthImageWidth()
    {
        return this.depthImageWidth;
    }

    public SimpleLoadOptionsModel requestDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth = depthImageWidth;
        return this;
    }

    @Override
    public int getDepthImageHeight()
    {
        return this.depthImageHeight;
    }

    public SimpleLoadOptionsModel requestDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight = depthImageHeight;
        return this;
    }

    @Override
    public int getMaxLoadingThreads()
    {
        return maxLoadingThreads;
    }

    @Override
    public void setMaxLoadingThreads(int maxLoadingThreads)
    {
        this.maxLoadingThreads = maxLoadingThreads;
    }
}
