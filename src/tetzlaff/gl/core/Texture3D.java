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

package tetzlaff.gl.core;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.function.Function;

import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import tetzlaff.gl.types.AbstractDataType;

/**
 * An interface for a three-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture3D<ContextType extends Context<ContextType>>
    extends Texture<ContextType>, Blittable<Texture3D<ContextType>>, Croppable<Texture3D<ContextType>>
{
    /**
     * Gets the width of the texture.
     * @return The width of the texture.
     */
    int getWidth();

    /**
     * Gets the height of the texture.
     * @return The height of the texture.
     */
    int getHeight();

    /**
     * Gets teh depth of the texture (the number of layers if used as a texture array).
     * @return The depth of the texture.
     */
    int getDepth();

    /**
     * Sets the texture wrap modes.
     * @param wrapS The horizontal wrap mode.
     * @param wrapT The vertical wrap mode.
     * @param wrapR the wrap mode in the depth dimension.
     */
    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT, TextureWrapMode wrapR);

    /**}
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param fileStream The stream from which to read the texture layer.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param file A file containing the image in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param imageStream A stream containing the image in a format supported by Java's ImageIO library.
     * @param maskStream A stream containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param imageFile A file containing the image in a format supported by Java's ImageIO library.
     * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * Maps the pixel data to another format before storing it in the texture layer.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param fileStream A stream containing the image in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    <MappedType> void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * Maps the pixel data to another format before storing it in the texture layer.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param file A file containing the image in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    <MappedType> void loadLayer(int layerIndex, File file, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * Maps the pixel data to another format before storing it in the texture layer.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param imageStream A stream containing the image in a format supported by Java's ImageIO library.
     * @param maskStream A stream containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    <MappedType> void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * Maps the pixel data to another format before storing it in the texture layer.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param imageFile A file containing the image in a format supported by Java's ImageIO library.
     * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
     * @param mappedType The type to which to map the data for storage in the texture.
     * @param mappingFunction The function that transforms the color from the image to the mapped type for storage in the texture.
     * @param <MappedType> The high-level return type of the mapping function.
     *                     This is typically either Number (for single-channel textures) or an Iterable of Numbers for multi-channel textures.
     * @throws IOException Upon a File I/O problem while loading the image.
     */
    <MappedType> void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    /**
     * Loads pixel data from a buffer and sends it to the GPU for a specific layer of the 3D texture, replacing whatever pixel data was there before.
     * @param layerIndex The index of the layer where the pixel data should go.
     * @param data The new pixel data to put in the texture.
     */
    void loadLayer(int layerIndex, ReadonlyNativeVectorBuffer data);

    /**
     * Gets a single layer of this texture for use as a framebuffer attachment.
     * @param layerIndex The layer to use as a framebuffer attachment.
     * @return An object encapsulating the use of a layer of this texture as a framebuffer attachment.
     */
    FramebufferAttachment<ContextType> getLayerAsFramebufferAttachment(int layerIndex);

    /**
     * Creates a new, empty texture with different dimensions but the same internal format and settings as this texture.
     * Especially intended to be used with framebuffer blitting to ensure compatibility.
     * @param newWidth The width of the new texture.
     * @param newHeight The height of the new texture.
     * @param newDepth The depth of the new texture.
     * @return The newly created texture.
     */
    Texture3D<ContextType> createTextureWithMatchingFormat(int newWidth, int newHeight, int newDepth);


    /**
     * Copies pixels from part of a 3D texture to another.  The copying operation will be start at (x, y) in layer z
     * within this texture, and resize if the requested source and destination rectangles are not the same size.
     * @param destX The left edge of the rectangle to copy into within this 3D texture.
     * @param destY The bottom edge of the rectangle to copy into within this 3D texture.
     * @param destZ The first layer to copy into within this 3D texture.
     * @param destWidth The width of the rectangle to copy at the destination resolution.
     * @param destHeight The height of the rectangle to copy at the destination resolution.
     * @param readSource The 3D texture source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcZ The first z-layer of the source to copy
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param srcDepth The number of layers of the source to copy.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     *                        If the texture is a depth or stencil texture, this will be ignored (linear filtering will be disabled).
     */
    default void blitCroppedAndScaled(int destX, int destY, int destZ, int destWidth, int destHeight,
        Texture3D<ContextType> readSource, int srcX, int srcY, int srcZ, int srcWidth, int srcHeight, int srcDepth,
        boolean linearFiltering)
    {
        FramebufferObjectBuilder<ContextType> fboBuilder = getContext().buildFramebufferObject(readSource.getWidth(), readSource.getHeight());

        if (this.getTextureType() == TextureType.COLOR)
        {
            // Color attachments need to be declared in advance; depth and stencil are assumed to always be a possibility
            fboBuilder.addEmptyColorAttachment();
        }

        try(FramebufferObject<ContextType> sourceFBO = fboBuilder.createFramebufferObject();
            FramebufferObject<ContextType> destFBO = fboBuilder.createFramebufferObject())
        {
            int effectiveDepth = Math.min(Math.min(this.getDepth() - destZ, readSource.getDepth() - srcZ), srcDepth);

            // Use framebuffer blitting on each depth layer requested
            for (int i = 0; i < effectiveDepth; i++)
            {
                switch(this.getTextureType())
                {
                    case COLOR:
                        sourceFBO.setColorAttachment(0, readSource.getLayerAsFramebufferAttachment(srcZ + i));
                        destFBO.setColorAttachment(0, this.getLayerAsFramebufferAttachment(destZ + i));
                        destFBO.blitColorAttachmentFromFramebufferViewport(0, destX, destY, destWidth, destHeight,
                            sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight), 0, linearFiltering);
                        break;
                    case DEPTH:
                    case FLOATING_POINT_DEPTH:
                        sourceFBO.setDepthAttachment(readSource.getLayerAsFramebufferAttachment(srcZ + i));
                        destFBO.setDepthAttachment(this.getLayerAsFramebufferAttachment(destZ + i));
                        destFBO.blitDepthAttachmentFromFramebufferViewport(destX, destY, destWidth, destHeight,
                            sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight));
                    case STENCIL:
                        sourceFBO.setStencilAttachment(readSource.getLayerAsFramebufferAttachment(srcZ + i));
                        destFBO.setStencilAttachment(this.getLayerAsFramebufferAttachment(destZ + i));
                        destFBO.blitStencilAttachmentFromFramebufferViewport(destX, destY, destWidth, destHeight,
                            sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight));
                    case DEPTH_STENCIL:
                    case FLOATING_POINT_DEPTH_STENCIL:
                        sourceFBO.setDepthStencilAttachment(readSource.getLayerAsFramebufferAttachment(srcZ + i));
                        destFBO.setDepthStencilAttachment(this.getLayerAsFramebufferAttachment(destZ + i));
                        destFBO.blitDepthStencilAttachmentFromFramebufferViewport(destX, destY, destWidth, destHeight,
                            sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight));
                }
            }
        }
    }

    /**
     * Copies pixels from part of a 3D texture to another.  The copying operation will be start at (x, y) in layer 0
     * within this texture, and resize if the requested source and destination rectangles are not the same size.
     * All layers of the source will be copied if there are sufficient destination layers to contain them.
     * @param destX The left edge of the rectangle to copy into within this 3D texture.
     * @param destY The bottom edge of the rectangle to copy into within this 3D texture.
     * @param destWidth The width of the rectangle to copy at the destination resolution.
     * @param destHeight The height of the rectangle to copy at the destination resolution.
     * @param readSource The 3D texture source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     */
    @Override
    default void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight,
        Texture3D<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
    {
        blitCroppedAndScaled(destX, destY, 0, destWidth, destHeight,
            readSource, srcX, srcY, 0, srcWidth, srcHeight, readSource.getDepth(), linearFiltering);
    }

    /**
     * Copies pixels from part of a 3D texture to another.  The copying operation will start at (0, 0) in layer 0
     * within this texture, and will preserve the resolution of the read source.
     * @param destX The left edge of the rectangle to copy into within this 3D texture.
     * @param destY The bottom edge of the rectangle to copy into within this 3D texture.
     * @param destZ The first layer to copy into within this 3D texture.
     * @param readSource The 3D texture source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcZ The first z-layer of the source to copy
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param srcDepth The number of layers of the source to copy.
     */
    default void blitCropped(int destX, int destY, int destZ,
        Texture3D<ContextType> readSource, int srcX, int srcY, int srcZ, int srcWidth, int srcHeight, int srcDepth)
    {
        blitCroppedAndScaled(destX, destY, destZ, srcWidth, srcHeight,
            readSource, srcX, srcY, srcZ, srcWidth, srcHeight, srcDepth, false);
    }

    /**
     * Copies pixels from part of a 3D texture to another.  The copying operation will start at (0, 0) in layer 0
     * within this texture, and will preserve the resolution of the read source.
     * @param readSource The 3D texture source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcZ The first z-layer of the source to copy
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param srcDepth The number of layers of the source to copy.
     */
    default void blitCropped(
        Texture3D<ContextType> readSource, int srcX, int srcY, int srcZ, int srcWidth, int srcHeight, int srcDepth)
    {
        blitCropped(0, 0, 0, readSource, srcX, srcY, srcZ, srcWidth, srcHeight, srcDepth);
    }

    /**
     * Creates a new texture that contains a cropped 3D region of this texture.
     * The texture this method is called on will remain unchanged.
     * @param x The left boundary of the cropped region
     * @param y The bottom boundary of the cropped region
     * @param z The lower depth boundary of the cropped region
     * @param cropWidth The width of the cropped region
     * @param cropHeight The height of the cropped region
     * @param cropDepth The depth of the cropped region
     * @return The new cropped texture.
     */
    default Texture3D<ContextType> crop(int x, int y, int z, int cropWidth, int cropHeight, int cropDepth)
    {
        Texture3D<ContextType> cropTexture = createTextureWithMatchingFormat(cropWidth, cropHeight, cropDepth);
        cropTexture.blitCropped(this, x, y, z, cropWidth, cropHeight, cropDepth);
        return cropTexture;
    }

    /**
     * Creates a new texture that contains a cropped 2D region of this texture.  All z-layers will be copied.
     * The texture this method is called on will remain unchanged.
     * @param x The left boundary of the cropped region
     * @param y The bottom boundary of the cropped region
     * @param cropWidth The width of the cropped region
     * @param cropHeight The height of the cropped region
     * @return The new cropped texture.
     */
    @Override
    default Texture3D<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        return this.crop(x, y, 0, cropWidth, cropHeight, this.getDepth());
    }

    /**
     * Copies this texture, creating a new resource with identical contents.
     * @return The new resource containing a copy of the texture
     */
    default Texture3D<ContextType> copy()
    {
        return this.crop(0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Gets an object that encapsulates read capabilities for this texture as a color texture.
     * @param layerIndex The index of the layer within the 3D texture to be read.
     * @return the texture reader
     */
    default ColorTextureReader getColorTextureReader(int layerIndex)
    {
        return new ColorTextureReaderBase()
        {
            @Override
            public int getWidth()
            {
                return Texture3D.this.getWidth();
            }

            @Override
            public int getHeight()
            {
                return Texture3D.this.getHeight();
            }

            @Override
            public void readARGB(ByteBuffer destination, int x, int y, int width, int height)
            {
                try(FramebufferObject<ContextType> fbo = getContext()
                    .buildFramebufferObject(this.getWidth(), this.getHeight())
                    .addEmptyColorAttachment()
                    .createFramebufferObject())
                {
                    fbo.setColorAttachment(0, Texture3D.this.getLayerAsFramebufferAttachment(layerIndex));
                    fbo.getTextureReaderForColorAttachment(0).readARGB(destination, x, y, width, height);
                }
            }

            @Override
            public void readFloatingPointRGBA(FloatBuffer destination, int x, int y, int width, int height)
            {
                try(FramebufferObject<ContextType> fbo = getContext()
                    .buildFramebufferObject(this.getWidth(), this.getHeight())
                    .addEmptyColorAttachment()
                    .createFramebufferObject())
                {
                    fbo.setColorAttachment(0, Texture3D.this.getLayerAsFramebufferAttachment(layerIndex));
                    fbo.getTextureReaderForColorAttachment(0).readFloatingPointRGBA(destination, x, y, width, height);
                }
            }

            @Override
            public void readIntegerRGBA(IntBuffer destination, int x, int y, int width, int height)
            {
                try(FramebufferObject<ContextType> fbo = getContext()
                    .buildFramebufferObject(this.getWidth(), this.getHeight())
                    .addEmptyColorAttachment()
                    .createFramebufferObject())
                {
                    fbo.setColorAttachment(0, Texture3D.this.getLayerAsFramebufferAttachment(layerIndex));
                    fbo.getTextureReaderForColorAttachment(0).readIntegerRGBA(destination, x, y, width, height);
                }
            }
        };
    }

    /**
     * Gets an object that encapsulates read capabilities for this texture as a depth texture.
     * @param layerIndex The index of the layer within the 3D texture to be read.
     * @return the texture reader
     */
    default DepthTextureReader getDepthTextureReader(int layerIndex)
    {
        return new DepthTextureReaderBase()
        {
            @Override
            public int getWidth()
            {
                return Texture3D.this.getWidth();
            }

            @Override
            public int getHeight()
            {
                return Texture3D.this.getHeight();
            }

            @Override
            public void read(ShortBuffer destination, int x, int y, int width, int height)
            {
                try(FramebufferObject<ContextType> fbo = getContext()
                    .buildFramebufferObject(this.getWidth(), this.getHeight()).createFramebufferObject())
                {
                    fbo.setDepthAttachment(Texture3D.this.getLayerAsFramebufferAttachment(layerIndex));
                    fbo.getTextureReaderForDepthAttachment().read(destination, x, y, width, height);
                }
            }
        };
    }
}
