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

package kintsugi3d.gl.core;

import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    extends Resource, FramebufferAttachment<ContextType>,
            Blittable<ReadonlyTexture2D<ContextType>>, ReadonlyTexture2D<ContextType>
{
    /**
     * Sets the texture wrap modes.
     * @param wrapS The horizontal wrap mode.
     * @param wrapT The vertical wrap mode.
     */
    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT);

    /**
     * Loads pixel data and sends it to the GPU, replacing whatever pixel data was there before.
     * @param imageStream The stream from which to read the texture.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically
     *                     to resolve discrepancies with respect to the orientation of the vertical axis.
     */
    void load(InputStream imageStream, boolean flipVertical) throws IOException;

    /**
     * Loads pixel data and sends it to the GPU, replacing whatever pixel data was there before.
     * @param imageFile A file containing the image in a format supported by Java's ImageIO library.
     * @param flipVertical Whether or not to automatically flip all of the pixels vertically
     *                     to resolve discrepancies with respect to the orientation of the vertical axis.
     */
    void load(File imageFile, boolean flipVertical) throws IOException;

    /**
     * Loads pixel data from a buffer and sends it to the GPU, replacing whatever pixel data was there before.
     * @param data The new pixel data to put in the texture.
     */
    void load(ReadonlyNativeVectorBuffer data);

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
    void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight,
        ReadonlyTexture2D<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering);

    @Override
    default Texture2D<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        Texture2D<ContextType> cropTexture = createTextureWithMatchingFormat(cropWidth, cropHeight);
        cropTexture.blitCropped(this, x, y, cropWidth, cropHeight);
        return cropTexture;
    }

    @Override
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

    @Override
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
