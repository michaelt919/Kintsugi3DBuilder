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

package kintsugi3d.gl.core;

import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * An interface for a two-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture2D<ContextType extends Context<ContextType>>
    extends Texture<ContextType>, FramebufferAttachment<ContextType>, Blittable<Texture2D<ContextType>>, Croppable<Texture2D<ContextType>>
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
     * Sets the texture wrap modes.
     * @param wrapS The horizontal wrap mode.
     * @param wrapT The vertical wrap mode.
     */
    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT);

    /**
     * Loads pixel data from a buffer and sends it to the GPU, replacing whatever pixel data was there before.
     * @param data The new pixel data to put in the texture.
     */
    void load(ReadonlyNativeVectorBuffer data);

    /**
     * Creates a new, empty texture with different dimensions but the same internal format and settings as this texture.
     * Especially intended to be used with framebuffer blitting to ensure compatibility.
     * @param newWidth The width of the new texture.
     * @param newHeight The height of the new texture
     * @return The newly created texture.
     */
    Texture2D<ContextType> createTextureWithMatchingFormat(int newWidth, int newHeight);

    /**
     * Copies pixels from part of a blittable to another.  The copying operation will be start at (x, y) within
     * this blittable, and resize if the requested source and destination rectangles are not the same size.
     * @param destX The left edge of the rectangle to copy into within this blittable.
     * @param destY The bottom edge of the rectangle to copy into within this blittable.
     * @param destWidth The width of the rectangle to copy at the destination resolution.
     * @param destHeight The height of the rectangle to copy at the destination resolution.
     * @param readSource The blittable source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     *                        If the texture is a depth or stencil texture, this will be ignored (linear filtering will be disabled).
     */
    @Override
    default void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight,
        Texture2D<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
    {
        if (this.isInternalFormatCompressed())
        {
            throw new UnsupportedOperationException("Cannot blit to a compressed texture.");
        }
        else if (readSource.isInternalFormatCompressed())
        {
            throw new UnsupportedOperationException("Cannot blit from a compressed texture.");
        }

        FramebufferObjectBuilder<ContextType> fboBuilder = getContext().buildFramebufferObject(readSource.getWidth(), readSource.getHeight());

        if (this.getTextureType() == TextureType.COLOR)
        {
            // Color attachments need to be declared in advance; depth and stencil are assumed to always be a possibility
            fboBuilder.addEmptyColorAttachment();
        }

        try(FramebufferObject<ContextType> sourceFBO = fboBuilder.createFramebufferObject();
            FramebufferObject<ContextType> destFBO = fboBuilder.createFramebufferObject())
        {
            switch(this.getTextureType())
            {
                case COLOR:
                    sourceFBO.setColorAttachment(0, readSource);
                    destFBO.setColorAttachment(0, this);
                    destFBO.getViewport(destX, destY, destWidth, destHeight)
                        .blitColorAttachmentFromFramebuffer(0, sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight), 0);
                    break;
                case DEPTH:
                case FLOATING_POINT_DEPTH:
                    sourceFBO.setDepthAttachment(readSource);
                    destFBO.setDepthAttachment(this);
                    destFBO.getViewport(destX, destY, destWidth, destHeight)
                        .blitDepthAttachmentFromFramebuffer(sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight));
                case STENCIL:
                    sourceFBO.setStencilAttachment(readSource);
                    destFBO.setStencilAttachment(this);
                    destFBO.getViewport(destX, destY, destWidth, destHeight)
                        .blitStencilAttachmentFromFramebuffer(sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight));
                case DEPTH_STENCIL:
                case FLOATING_POINT_DEPTH_STENCIL:
                    sourceFBO.setDepthStencilAttachment(readSource);
                    destFBO.setDepthStencilAttachment(this);
                    destFBO.getViewport(destX, destY, destWidth, destHeight)
                        .blitDepthStencilAttachmentFromFramebuffer(sourceFBO.getViewport(srcX, srcY, srcWidth, srcHeight));
            }
        }
    }

    /**
     * Creates a new texture that contains a cropped region of this texture.
     * The texture this method is called on will remain unchanged.
     * @param x The left boundary of the cropped region
     * @param y The bottom boundary of the cropped region
     * @param cropWidth The width of the cropped region
     * @param cropHeight The height of the cropped region
     * @return The new cropped texture.
     */
    @Override
    default Texture2D<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        Texture2D<ContextType> cropTexture = createTextureWithMatchingFormat(cropWidth, cropHeight);
        cropTexture.blitCropped(this, x, y, cropWidth, cropHeight);
        return cropTexture;
    }

    /**
     * Copies this texture, creating a new resource with identical contents.
     * @return The new resource containing a copy of the texture
     */
    default Texture2D<ContextType> copy()
    {
        return this.crop(0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Gets an object that encapsulates read capabilities for this texture as a color texture.
     * @return the texture reader
     */
    default ColorTextureReader getColorTextureReader()
    {
        return new ColorTextureReaderBase()
        {
            @Override
            public int getWidth()
            {
                return Texture2D.this.getWidth();
            }

            @Override
            public int getHeight()
            {
                return Texture2D.this.getHeight();
            }

            @Override
            public void readARGB(ByteBuffer destination, int x, int y, int width, int height)
            {
                try(FramebufferObject<ContextType> fbo = getContext()
                    .buildFramebufferObject(this.getWidth(), this.getHeight())
                    .addEmptyColorAttachment()
                    .createFramebufferObject())
                {
                    fbo.setColorAttachment(0, Texture2D.this);
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
                    fbo.setColorAttachment(0, Texture2D.this);
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
                    fbo.setColorAttachment(0, Texture2D.this);
                    fbo.getTextureReaderForColorAttachment(0).readIntegerRGBA(destination, x, y, width, height);
                }
            }
        };
    }

    /**
     * Gets an object that encapsulates read capabilities for this texture as a depth texture.
     * @return the texture reader
     */
    default DepthTextureReader getDepthTextureReader()
    {
        return new DepthTextureReaderBase()
        {
            @Override
            public int getWidth()
            {
                return Texture2D.this.getWidth();
            }

            @Override
            public int getHeight()
            {
                return Texture2D.this.getHeight();
            }

            @Override
            public void read(ShortBuffer destination, int x, int y, int width, int height)
            {
                try(FramebufferObject<ContextType> fbo = getContext()
                    .buildFramebufferObject(this.getWidth(), this.getHeight())
                    .createFramebufferObject())
                {
                    fbo.setDepthAttachment(Texture2D.this);
                    fbo.getTextureReaderForDepthAttachment().read(destination, x, y, width, height);
                }
            }
        };
    }
}
