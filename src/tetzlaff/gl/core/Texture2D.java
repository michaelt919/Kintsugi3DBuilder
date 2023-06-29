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

import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

/**
 * An interface for a two-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture2D<ContextType extends Context<ContextType>>
    extends Texture<ContextType>, FramebufferAttachment<ContextType>, Croppable<Texture2D<ContextType>>, CloneCroppable<Texture2D<ContextType>>
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
    void load(NativeVectorBuffer data);

    /**
     * Creates a new, empty texture with different dimensions but the same internal format and settings as this texture.
     * Especially intended to be used with framebuffer blitting to ensure compatibility.
     * @param newWidth The width of the new texture.
     * @param newHeight The height of the new texture
     * @return The newly created texture.
     */
    Texture2D<ContextType> createTextureWithMatchingFormat(int newWidth, int newHeight);

    @Override
    default
    void cropFrom(Texture2D<ContextType> other, int x, int y, int cropWidth, int cropHeight)
    {
        try(FramebufferObject<ContextType> sourceFBO = getContext().buildFramebufferObject(other.getWidth(), other.getHeight()).createFramebufferObject();
            FramebufferObject<ContextType> destFBO = getContext().buildFramebufferObject(cropWidth, cropHeight).createFramebufferObject())
        {
            sourceFBO.setColorAttachment(0, other);
            destFBO.setColorAttachment(0, this);
            destFBO.blitColorAttachmentFromFramebuffer(0, sourceFBO.getViewport(x, y, cropWidth, cropHeight), 0);
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
        cropTexture.cropFrom(this, x, y, cropWidth, cropHeight);
        return cropTexture;
    }

    /**
     * Copies another texture into this texture using framebuffer blitting
     * @param other The texture to copy.
     */
    default void copyFrom(Texture2D<ContextType> other)
    {
        cropFrom(other, 0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Copies this texture, creating a new resource with identical contents.
     * @return The new resource containing a copy of the texture
     */
    default Texture2D<ContextType> copy()
    {
        return this.crop(0, 0, this.getWidth(), this.getHeight());
    }
}
