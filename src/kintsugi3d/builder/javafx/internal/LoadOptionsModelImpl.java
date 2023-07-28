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

package kintsugi3d.builder.javafx.internal;//Created by alexk on 8/1/2017.

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;

public class LoadOptionsModelImpl implements ReadonlyLoadOptionsModel
{
    public final BooleanProperty colorImages = new SimpleBooleanProperty(true);
    public final BooleanProperty mipmaps = new SimpleBooleanProperty(true);
    public final BooleanProperty compression = new SimpleBooleanProperty(true);
    public final BooleanProperty alpha = new SimpleBooleanProperty(true);
    public final BooleanProperty depthImages = new SimpleBooleanProperty(true);
    public final IntegerProperty depthWidth = new SimpleIntegerProperty(1024);
    public final IntegerProperty depthHeight = new SimpleIntegerProperty(1024);
    public final IntegerProperty previewWidth = new SimpleIntegerProperty(1024);
    public final IntegerProperty previewHeight = new SimpleIntegerProperty(1024);

    @Override
    public boolean areColorImagesRequested()
    {
        return colorImages.get();
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return mipmaps.get();
    }

    @Override
    public boolean isCompressionRequested()
    {
        return compression.get();
    }

    @Override
    public boolean isAlphaRequested()
    {
        return alpha.get();
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return depthImages.get();
    }

    @Override
    public int getDepthImageWidth()
    {
        return depthWidth.get();
    }

    @Override
    public int getDepthImageHeight()
    {
        return depthHeight.get();
    }

    @Override
    public int getPreviewImageWidth()
    {
        return previewWidth.get();
    }

    @Override
    public int getPreviewImageHeight()
    {
        return previewHeight.get();
    }
}
