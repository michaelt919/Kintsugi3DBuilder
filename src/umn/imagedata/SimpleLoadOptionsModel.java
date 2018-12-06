/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.imagedata;

public class SimpleLoadOptionsModel implements ReadonlyLoadOptionsModel
{
    private boolean colorImagesRequested;
    private boolean mipmapsRequested;
    private boolean compressionRequested;
    private boolean alphaRequested;
    private boolean depthImagesRequested;
    private int depthImageWidth;
    private int depthImageHeight;

    @Override
    public boolean areColorImagesRequested()
    {
        return this.colorImagesRequested;
    }

    public SimpleLoadOptionsModel setColorImagesRequested(boolean colorImagesRequested)
    {
        this.colorImagesRequested = colorImagesRequested;
        return this;
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return this.mipmapsRequested;
    }

    public SimpleLoadOptionsModel setMipmapsRequested(boolean mipmapsRequested)
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

    public SimpleLoadOptionsModel setAlphaRequested(boolean alphaRequested)
    {
        this.alphaRequested = alphaRequested;
        return this;
    }

    public SimpleLoadOptionsModel setCompressionRequested(boolean compressionRequested)
    {
        this.compressionRequested = compressionRequested;
        return this;
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return this.depthImagesRequested;
    }

    public SimpleLoadOptionsModel setDepthImagesRequested(boolean depthImagesRequested)
    {
        this.depthImagesRequested = depthImagesRequested;
        return this;
    }

    @Override
    public int getDepthImageWidth()
    {
        return this.depthImageWidth;
    }

    public SimpleLoadOptionsModel setDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth = depthImageWidth;
        return this;
    }

    @Override
    public int getDepthImageHeight()
    {
        return this.depthImageHeight;
    }

    public SimpleLoadOptionsModel setDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight = depthImageHeight;
        return this;
    }
}
