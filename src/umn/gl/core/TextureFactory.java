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

package umn.gl.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.Function;

import umn.gl.builders.*;
import umn.gl.nativebuffer.NativeVectorBuffer;
import umn.gl.types.AbstractDataType;

public interface TextureFactory<ContextType extends Context<ContextType>>
{
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStreamWithMask(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

    <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStreamWithMask(
        InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorHDRTextureFromStreamWithMask(BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImageWithMask(BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical);

    <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImageWithMask(
        BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction);

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStream(InputStream imageStream, boolean flipVertical) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(imageStream, null, flipVertical);
    }

    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStream(
        InputStream imageStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(imageStream, null, flipVertical, mappedType, mappingFunction);
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromFileWithMask(File imageFile, File maskFile, boolean flipVertical) throws IOException
    {
        if (imageFile.getName().endsWith(".hdr"))
        {
            return build2DColorHDRTextureFromStreamWithMask(new BufferedInputStream(new FileInputStream(imageFile)), new FileInputStream(maskFile), flipVertical);
        }
        else
        {
            return build2DColorTextureFromStreamWithMask(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
        }
    }

    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromFileWithMask(
        File imageFile, File maskFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical,
            mappedType, mappingFunction);
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorHDRTextureFromStream(BufferedInputStream imageStream, boolean flipVertical) throws IOException
    {
        return build2DColorHDRTextureFromStreamWithMask(imageStream, null, flipVertical);
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromFile(File imageFile, boolean flipVertical) throws IOException
    {
        if (imageFile.getName().endsWith(".hdr"))
        {
            return build2DColorHDRTextureFromStream(new BufferedInputStream(new FileInputStream(imageFile)), flipVertical);
        }
        else
        {
            return build2DColorTextureFromStream(new FileInputStream(imageFile), flipVertical);
        }
    }

    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromFile(File imageFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return build2DColorTextureFromStream(new FileInputStream(imageFile), flipVertical, mappedType, mappingFunction);
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImage(BufferedImage colorImage, boolean flipVertical)
    {
        return build2DColorTextureFromImageWithMask(colorImage, null, flipVertical);
    }

    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImage(
        BufferedImage colorImage, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction)
    {
        return build2DColorTextureFromImageWithMask(colorImage, null, flipVertical, mappedType, mappingFunction);
    }

    ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> build1DColorTexture(NativeVectorBuffer data);

    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DColorTextureFromBuffer(int width, int height, NativeVectorBuffer data);
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DColorTexture(int width, int height);
    DepthTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DDepthTexture(int width, int height);
    StencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DStencilTexture(int width, int height);
    DepthStencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DDepthStencilTexture(int width, int height);

    TextureBuilder<ContextType, ? extends Texture2D<ContextType>> buildPerlinNoiseTexture();

    ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DColorTextureArray(int width, int height, int length);
    DepthTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DDepthTextureArray(int width, int height, int length);
    StencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DStencilTextureArray(int width, int height, int length);
    DepthStencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DDepthStencilTextureArray(int width, int height, int length);

    ColorCubemapBuilder<ContextType, ? extends Cubemap<ContextType>> buildColorCubemap(int faceSize);
    DepthTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildDepthCubemap(int faceSize);
    StencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildStencilCubemap(int faceSize);
    DepthStencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildDepthStencilCubemap(int faceSize);

    Texture<ContextType> getNullTexture(SamplerType samplerType);
}
