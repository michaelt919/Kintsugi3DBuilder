/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;//Created by alexk on 7/31/2017.

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import kintsugi3d.gl.builders.ColorTextureBuilder;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.CompressionFormat;
import kintsugi3d.gl.material.TextureLoadOptions;

/**
 * For serialization purposes, this class should always return primitives
 */
@JsonSerialize(as = LoadOptionsModel.class)
public interface ReadonlyLoadOptionsModel
{
    boolean areColorImagesRequested();
    boolean areMipmapsRequested();
    boolean isCompressionRequested();
    boolean isAlphaRequested();
    int getMaxLoadingThreads();
    boolean areDepthImagesRequested();
    int getDepthImageWidth();
    int getDepthImageHeight();
    int getPreviewImageWidth();
    int getPreviewImageHeight();

    @JsonIgnore
    default TextureLoadOptions getTextureLoadOptions()
    {
        TextureLoadOptions options = new TextureLoadOptions();
        options.setMipmapsRequested(areMipmapsRequested());
        options.setCompressionRequested(isCompressionRequested());
        return options;
    }

    default <BuilderType extends ColorTextureBuilder<?,?>> void configureColorTextureBuilder(BuilderType colorTextureBuilder)
    {
        if (this.isCompressionRequested())
        {
            if (this.isAlphaRequested())
            {
                colorTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP_ALPHA_4BPP);
            }
            else
            {
                colorTextureBuilder.setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP);
            }
        }
        else
        {
            colorTextureBuilder.setInternalFormat(ColorFormat.RGBA8);
        }

        colorTextureBuilder
            .setMipmapsEnabled(this.areMipmapsRequested())
            .setLinearFilteringEnabled(true)
            .setMaxAnisotropy(16.0f);
    }
}
