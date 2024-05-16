/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.core;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = LoadOptionsModel.class)
@JsonDeserialize(as = SimpleLoadOptionsModel.class)
public interface LoadOptionsModel extends ReadonlyLoadOptionsModel
{
    void setColorImagesRequested(boolean colorImagesRequested);
    void setMipmapsRequested(boolean mipmapsRequested);
    void setCompressionRequested(boolean compressionRequested);
    void setMaxLoadingThreads(int maxLoadingThreads);
    void setDepthImagesRequested(boolean depthImagesRequested);
    void setDepthImageWidth(int depthImageWidth);
    void setDepthImageHeight(int depthImageHeight);
    void setPreviewImageHeight(int previewImageHeight);
    void setPreviewImageWidth(int previewImageWidth);
    default void copyFrom(ReadonlyLoadOptionsModel otherModel)
    {
        setColorImagesRequested(otherModel.areColorImagesRequested());
        setMipmapsRequested(otherModel.areMipmapsRequested());
        setCompressionRequested(otherModel.isCompressionRequested());
        setDepthImagesRequested(otherModel.areDepthImagesRequested());
        setDepthImageWidth(otherModel.getDepthImageWidth());
        setDepthImageHeight(otherModel.getDepthImageHeight());
        setPreviewImageHeight(otherModel.getPreviewImageHeight());
        setPreviewImageWidth(otherModel.getPreviewImageWidth());
    }
}
