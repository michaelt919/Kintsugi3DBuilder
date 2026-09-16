/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.multithread;

import kintsugi3d.builder.io.LoadOptionsModel;

public class SynchronizedLoadOptionsModel implements LoadOptionsModel
{
    private final SynchronizedValue<Boolean> colorImagesRequested;
    private final SynchronizedValue<Boolean> mipmapsRequested;
    private final SynchronizedValue<Boolean> compressionRequested;
    private final SynchronizedValue<Boolean> alphaRequested;
    private final SynchronizedValue<Integer> maxLoadingThreads;
    private final SynchronizedValue<Boolean> depthImagesRequested;
    private final SynchronizedValue<Integer> depthImageWidth;
    private final SynchronizedValue<Integer> depthImageHeight;
    private final SynchronizedValue<Integer> previewImageWidth;
    private final SynchronizedValue<Integer> previewImageHeight;

    public SynchronizedLoadOptionsModel(LoadOptionsModel baseModel)
    {
        this.colorImagesRequested = SynchronizedValue.createFromFunctions(baseModel::areColorImagesRequested, baseModel::setColorImagesRequested);
        this.mipmapsRequested = SynchronizedValue.createFromFunctions(baseModel::areMipmapsRequested, baseModel::setMipmapsRequested);
        this.compressionRequested = SynchronizedValue.createFromFunctions(baseModel::isCompressionRequested, baseModel::setCompressionRequested);
        this.alphaRequested = SynchronizedValue.createFromFunctions(baseModel::isAlphaRequested, baseModel::setAlphaRequested);
        this.maxLoadingThreads = SynchronizedValue.createFromFunctions(baseModel::getMaxLoadingThreads, baseModel::setMaxLoadingThreads);
        this.depthImagesRequested = SynchronizedValue.createFromFunctions(baseModel::areDepthImagesRequested,  baseModel::setDepthImagesRequested);
        this.depthImageWidth = SynchronizedValue.createFromFunctions(baseModel::getDepthImageWidth, baseModel::setDepthImageWidth);
        this.depthImageHeight = SynchronizedValue.createFromFunctions(baseModel::getDepthImageHeight, baseModel::setDepthImageHeight);
        this.previewImageWidth = SynchronizedValue.createFromFunctions(baseModel::getPreviewImageWidth, baseModel::setPreviewImageWidth);
        this.previewImageHeight = SynchronizedValue.createFromFunctions(baseModel::getPreviewImageHeight, baseModel::setPreviewImageHeight);
    }

    @Override
    public boolean areColorImagesRequested()
    {
        return colorImagesRequested.getValue();
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return mipmapsRequested.getValue();
    }

    @Override
    public boolean isCompressionRequested()
    {
        return compressionRequested.getValue();
    }

    @Override
    public boolean isAlphaRequested()
    {
        return alphaRequested.getValue();
    }

    @Override
    public int getMaxLoadingThreads()
    {
        return maxLoadingThreads.getValue();
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return depthImagesRequested.getValue();
    }

    @Override
    public int getDepthImageWidth()
    {
        return depthImageWidth.getValue();
    }

    @Override
    public int getDepthImageHeight()
    {
        return depthImageHeight.getValue();
    }

    @Override
    public int getPreviewImageWidth()
    {
        return previewImageWidth.getValue();
    }

    @Override
    public int getPreviewImageHeight()
    {
        return previewImageHeight.getValue();
    }

    @Override
    public void setColorImagesRequested(boolean colorImagesRequested)
    {
        this.colorImagesRequested.setValue(colorImagesRequested);
    }

    @Override
    public void setMipmapsRequested(boolean mipmapsRequested)
    {
        this.mipmapsRequested.setValue(mipmapsRequested);
    }

    @Override
    public void setCompressionRequested(boolean compressionRequested)
    {
        this.compressionRequested.setValue(compressionRequested);
    }

    @Override
    public void setAlphaRequested(boolean alphaRequested)
    {
        this.alphaRequested.setValue(alphaRequested);
    }

    @Override
    public void setMaxLoadingThreads(int maxLoadingThreads)
    {
        this.maxLoadingThreads.setValue(maxLoadingThreads);
    }

    @Override
    public void setDepthImagesRequested(boolean depthImagesRequested)
    {
        this.depthImagesRequested.setValue(depthImagesRequested);
    }

    @Override
    public void setDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth.setValue(depthImageWidth);
    }

    @Override
    public void setDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight.setValue(depthImageHeight);
    }

    @Override
    public void setPreviewImageHeight(int previewImageHeight)
    {
        this.previewImageHeight.setValue(previewImageHeight);
    }

    @Override
    public void setPreviewImageWidth(int previewImageWidth)
    {
        this.previewImageWidth.setValue(previewImageWidth);
    }
}
