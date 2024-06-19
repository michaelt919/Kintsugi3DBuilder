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

package kintsugi3d.gl.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.Function;

import kintsugi3d.gl.builders.*;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.types.AbstractDataType;

public interface TextureFactory<ContextType extends Context<ContextType>>
{
    /**
     * Gets a builder object for a 2D color texture to be loaded from an arbitrary input stream along with a separate stream containing an alpha mask.
     * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
     * @param maskStream An input stream containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStreamWithMask(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

    /**
     * Gets a builder object for a 2D color texture to be loaded from an arbitrary input stream along with a separate stream containing an alpha mask.
     * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
     * @param maskStream An input stream containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStreamWithMask(
        InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    /**
     * Gets a builder object for a 2D HDR color texture to be loaded from an arbitrary input stream along with a separate stream containing an alpha mask.
     * @param imageStream An input stream containing the HDR image.
     * @param maskStream An input stream containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorHDRTextureFromStreamWithMask(BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

    /**
     * Gets a builder object for a 2D color texture to be loaded from an image along with a separate mask image.
     * @param colorImage The texture image.
     * @param maskImage The texture mask.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     */
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImageWithMask(BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical);

    /**
     * Gets a builder object for a 2D color texture to be loaded from an image along with a separate mask image.
     * @param colorImage The texture image.
     * @param maskImage The texture mask.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @return The builder object for the texture.
     */
    <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImageWithMask(
        BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction);

    /**
     * Gets a builder object for a 2D color texture to be loaded from an arbitrary input stream.
     * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStream(InputStream imageStream, boolean flipVertical) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(imageStream, null, flipVertical);
    }

    /**
     * Gets a builder object for a 2D color texture to be loaded from an arbitrary input stream.
     * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromStream(
        InputStream imageStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(imageStream, null, flipVertical, mappedType, mappingFunction);
    }

    /**
     * Gets a builder object for a 2D color texture to be loaded from a file along with an alpha mask in a separate file.
     * @param imageFile A file containing the image in either an HDR format or a format supported by Java's ImageIO library.
     * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
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

    /**
     * Gets a builder object for a 2D color texture to be loaded from a file along with an alpha mask in a separate file.
     * @param imageFile A file containing the image in either an HDR format or a format supported by Java's ImageIO library.
     * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromFileWithMask(
        File imageFile, File maskFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical,
            mappedType, mappingFunction);
    }

    /**
     * Gets a builder object for a 2D HDR color texture to be loaded from an arbitrary input stream,
     * @param imageStream An input stream containing the HDR image.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorHDRTextureFromStream(BufferedInputStream imageStream, boolean flipVertical) throws IOException
    {
        return build2DColorHDRTextureFromStreamWithMask(imageStream, null, flipVertical);
    }

    /**
     * Gets a builder object for a 2D color texture to be loaded from a file.
     * @param imageFile A file containing the image in either an HDR format or a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
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

    /**
     * Gets a builder object for a 2D color texture to be loaded from a file.
     * @param imageFile A file containing the image in either an HDR format or a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @return The builder object for the texture.
     * @throws IOException Upon a File I/O problem when reading the images.
     */
    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromFile(File imageFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return build2DColorTextureFromStream(new FileInputStream(imageFile), flipVertical, mappedType, mappingFunction);
    }

    /**
     * Gets a builder object for a 2D color texture to be loaded from an image.
     * @param colorImage The texture image.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @return The builder object for the texture.
     */
    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImage(BufferedImage colorImage, boolean flipVertical)
    {
        return build2DColorTextureFromImageWithMask(colorImage, null, flipVertical);
    }

    /**
     * Gets a builder object for a 2D color texture to be loaded from an image.
     * @param colorImage The texture image.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @return The builder object for the texture.
     */
    default <MappedType> ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
    build2DColorTextureFromImage(
        BufferedImage colorImage, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction)
    {
        return build2DColorTextureFromImageWithMask(colorImage, null, flipVertical, mappedType, mappingFunction);
    }

    /**
     * Gets a builder object for a AD color texture to be loaded from a memory buffer with a defined format (an array of vectors).
     * @param data The buffer containing the texture data.
     * @return The builder object for the texture.
     */
    ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> build1DColorTexture(ReadonlyNativeVectorBuffer data);


    /**
     * Gets a builder object for a 2D color texture to be loaded from a memory buffer with a defined format (an array of vectors).
     * @param width The width of the image.
     * @param height The height of the image.
     * @param data The buffer containing the texture data.
     * @return The builder object for the texture.
     */
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DColorTextureFromBuffer(int width, int height, ReadonlyNativeVectorBuffer data);

    /**
     * Gets a builder object for a blank 2D color texture.
     * @param width The width of the texture.
     * @param height The height of the texture.
     * @return The builder for the texture with the specified dimensions.
     */
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DColorTexture(int width, int height);

    /**
     * Gets a builder object for a blank 1D color texture array.
     * @param width The width of each element of the texture.
     * @param height The height of the texture, i.e. the number of elements in the array.
     * @return The builder for the texture with the specified dimensions.
     */
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build1DColorTextureArray(int width, int height);

    /**
     * Gets a builder object for a blank 2D depth texture.
     * @param width The width of the texture.
     * @param height The height of the texture.
     * @return The builder for the texture with the specified dimensions.
     */
    DepthTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DDepthTexture(int width, int height);

    /**
     * Gets a builder object for a blank 2D stencil texture.
     * @param width The width of the texture.
     * @param height The height of the texture.
     * @return The builder for the texture with the specified dimensions.
     */
    StencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DStencilTexture(int width, int height);

    /**
     * Gets a builder object for a blank 2D depth+stencil texture.
     * @param width The width of the texture.
     * @param height The height of the texture.
     * @return The builder for the texture with the specified dimensions.
     */
    DepthStencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DDepthStencilTexture(int width, int height);

    /**
     * Gets a builder object for a 2D texture to contain Perlin noise.
     * @return The builder for the texture.
     */
    TextureBuilder<ContextType, ? extends Texture2D<ContextType>> buildPerlinNoiseTexture();

    /**
     * Gets a builder object for a blank 2D color texture array.
     * @param width The width of each texture in the array.
     * @param height The height of each texture in the array.
     * @param length The length of the texture array.
     * @return The builder for the texture with the specified dimensions.
     */
    ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DColorTextureArray(int width, int height, int length);

    /**
     * Gets a builder object for a blank 2D depth texture array.
     * @param width The width of each texture in the array.
     * @param height The height of each texture in the array.
     * @param length The length of the texture array.
     * @return The builder for the texture with the specified dimensions.
     */
    DepthTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DDepthTextureArray(int width, int height, int length);

    /**
     * Gets a builder object for a blank 2D stencil texture array.
     * @param width The width of each texture in the array.
     * @param height The height of each texture in the array.
     * @param length The length of the texture array.
     * @return The builder for the texture with the specified dimensions.
     */
    StencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DStencilTextureArray(int width, int height, int length);

    /**
     * Gets a builder object for a blank 2D depth+stencil texture array.
     * @param width The width of each texture in the array.
     * @param height The height of each texture in the array.
     * @param length The length of the texture array.
     * @return The builder for the texture with the specified dimensions.
     */
    DepthStencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DDepthStencilTextureArray(int width, int height, int length);

    /**
     * Gets a builder object for a blank color cubemap.
     * @param faceSize The size of each face in the cubemap.
     * @return The builder for the texture with the specified dimensions.
     */
    ColorCubemapBuilder<ContextType, ? extends Cubemap<ContextType>> buildColorCubemap(int faceSize);

    /**
     * Gets a builder object for a blank depth cubemap.
     * @param faceSize The size of each face in the cubemap.
     * @return The builder for the texture with the specified dimensions.
     */
    DepthTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildDepthCubemap(int faceSize);

    /**
     * Gets a builder object for a blank stencil cubemap.
     * @param faceSize The size of each face in the cubemap.
     * @return The builder for the texture with the specified dimensions.
     */
    StencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildStencilCubemap(int faceSize);

    /**
     * Gets a builder object for a blank depth+stencil cubemap.
     * @param faceSize The size of each face in the cubemap.
     * @return The builder for the texture with the specified dimensions.
     */
    DepthStencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildDepthStencilCubemap(int faceSize);

    /**
     * Gets a null texture to represent the absence of a texture in certain contexts.
     * For instance, this can be used to unbind a texture from a shader variable.
     * @param samplerType The sampler type associated with this null texture.
     * @return The null texture with an associated sampler type.
     */
    Texture<ContextType> getNullTexture(SamplerType samplerType);
}
