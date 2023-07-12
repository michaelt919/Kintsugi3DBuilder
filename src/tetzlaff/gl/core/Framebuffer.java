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

import tetzlaff.gl.vecmath.IntVector2;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * An interface for a framebuffer.
 * This could be either the default on-screen framebuffer, or a framebuffer object (FBO).
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the framebuffer is associated with.
 */
public interface Framebuffer<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
    /**
     * Gets a representation of the contents of this framebuffer fr reading.
     * @return A handle that can be used to perform operations that retrieve the contents of this framebuffer.
     */
    FramebufferReadContents<ContextType> getReadContents();

    /**
     * Gets a representation of the contents of this framebuffer .
     * @return A handle that can be used to perform operations that modify the contents of this framebuffer.
     */
    FramebufferDrawContents<ContextType> getDrawContents();

    /**
     * Gets the dimensions of the framebuffer (width and height)
     * @return An object containing the framebuffer dimensions.
     */
    FramebufferSize getSize();

    /**
     * Gets the number of color attachments that this framebuffer has.
     * @return The number of color attachments that this framebuffer has.
     */
    int getColorAttachmentCount();

    /**
     * Gets an object that encapsulates read capabilities for this texture as a color texture.
     * @return the texture reader
     */
    ColorTextureReader getTextureReaderForColorAttachment(int attachmentIndex);

    /**
     * Gets an object that encapsulates read capabilities for this texture as a depth texture.
     * @return the texture reader
     */
    DepthTextureReader getTextureReaderForDepthAttachment();

    /**
     * Reads the pixels currently in one of the framebuffer's attachments.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    @Deprecated default void readColorBufferARGB(int attachmentIndex, ByteBuffer destination)
    {
        this.getTextureReaderForColorAttachment(attachmentIndex).readARGB(destination);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    @Deprecated default void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination)
    {
        this.getTextureReaderForColorAttachment(attachmentIndex).readFloatingPointRGBA(destination);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as 32-bit integers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    @Deprecated default void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination)
    {
        this.getTextureReaderForColorAttachment(attachmentIndex).readIntegerRGBA(destination);
    }

    /**
     * Reads the pixels currently in the framebuffer's depth attachment.
     * The entire framebuffer will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    @Deprecated default void readDepthBuffer(ShortBuffer destination)
    {
        this.getTextureReaderForDepthAttachment().read(destination);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @return An array containing the pixels.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    @Deprecated default int[] readColorBufferARGB(int attachmentIndex)
    {
        return this.getTextureReaderForColorAttachment(attachmentIndex).readARGB();
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @return An array containing the pixels as floating point numbers.
     */
    @Deprecated default float[] readFloatingPointColorBufferRGBA(int attachmentIndex)
    {
        return this.getTextureReaderForColorAttachment(attachmentIndex).readFloatingPointRGBA();
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as 32-bit integers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @return An array containing the pixels as floating point numbers.
     */
    @Deprecated default int[] readIntegerColorBufferRGBA(int attachmentIndex)
    {
        return this.getTextureReaderForColorAttachment(attachmentIndex).readIntegerRGBA();
    }

    /**
     * Reads the pixels currently in the framebuffer's depth attachment.
     * The entire framebuffer will be read.
     * @return An array containing the depth values.
     */
    @Deprecated default short[] readDepthBuffer()
    {
        return this.getTextureReaderForDepthAttachment().read();
    }

    /**
     * Saves the the pixels currently in one of the framebuffer's attachments to a file.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    @Deprecated default void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException
    {
        this.getTextureReaderForColorAttachment(attachmentIndex).saveToFile(fileFormat, file);
    }

    /**
     * Clears one of the framebuffer's color attachments.
     * @param attachmentIndex The index of the framebuffer attachment to be cleared.
     * @param r The red component of the clear color.
     * @param g The green component of the clear color.
     * @param b The blue component of the clear color.
     * @param a The alpha component of the clear color.
     */
    default void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a)
    {
        FramebufferSize size = this.getSize();
        clearColorBuffer(attachmentIndex, r, g, b, a, 0, 0, size.width, size.height);
    }

    /**
     * Clears one of the framebuffer's color attachments.
     * @param attachmentIndex The index of the framebuffer attachment to be cleared.
     * @param r The red component of the clear color.
     * @param g The green component of the clear color.
     * @param b The blue component of the clear color.
     * @param a The alpha component of the clear color.
     */
    default void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a)
    {
        FramebufferSize size = this.getSize();
        clearIntegerColorBuffer(attachmentIndex, r, g, b, a, 0, 0, size.width, size.height);
    }

    /**
     * Clears the framebuffer's depth attachment.
     * @param depth The depth to assign to the entire framebuffer.
     */
    default void clearDepthBuffer(float depth)
    {
        FramebufferSize size = this.getSize();
        clearDepthBuffer(depth, 0, 0, size.width, size.height);
    }

    /**
     * Clears the framebuffer's depth attachment to the far plane distance.
     */
    default void clearDepthBuffer()
    {
        this.clearDepthBuffer(1.0f);
    }

    /**
     * Clears the framebuffer's stencil buffer.
     * @param stencilIndex The stencil index with which to clear.
     */
    default void clearStencilBuffer(int stencilIndex)
    {
        FramebufferSize size = this.getSize();
        clearStencilBuffer(stencilIndex, 0, 0, size.width, size.height);
    }

    /**
     * Clears a rectangle within one of the framebuffer's color attachments.
     * @param attachmentIndex The index of the framebuffer attachment to be cleared.
     * @param r The red component of the clear color.
     * @param g The green component of the clear color.
     * @param b The blue component of the clear color.
     * @param a The alpha component of the clear color.
     */
    void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a, int x, int y, int width, int height);

    /**
     * Clears a rectangle within one of the framebuffer's color attachments.
     * @param attachmentIndex The index of the framebuffer attachment to be cleared.
     * @param r The red component of the clear color.
     * @param g The green component of the clear color.
     * @param b The blue component of the clear color.
     * @param a The alpha component of the clear color.
     */
    void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a, int x, int y, int width, int height);

    /**
     * Clears a rectangle within the framebuffer's depth attachment.
     * @param depth The depth to assign to the entire framebuffer.
     */
    void clearDepthBuffer(float depth, int x, int y, int width, int height);

    /**
     * Clears a rectangle within the framebuffer's stencil buffer.
     * @param stencilIndex The stencil index with which to clear.
     */
    void clearStencilBuffer(int stencilIndex, int x, int y, int width, int height);

    /**
     * Gets an object that encapsulates a viewport within this framebuffer that can be drawn to.
     * @param x The left edge of the viewport
     * @param y The bottom edge of the viewport
     * @param width The width of the viewport
     * @param height The height of the viewport
     * @return
     */
    default FramebufferViewport<ContextType> getViewport(int x, int y, int width, int height)
    {
        return new FramebufferViewport<>(this, new IntVector2(x, y), new FramebufferSize(width, height));
    }

    /**
     * Copies pixels from a viewport within one framebuffer to another framebuffer.
     * @param drawAttachmentIndex The index of the color attachment of this framebuffer to copy into.
     * @param destX The left edge of the rectangle to copy into within this framebuffer.
     * @param destY The bottom edge of the rectangle to copy into within this framebuffer.
     * @param destWidth The width of the rectangle to copy into within this framebuffer.
     * @param destHeight The height of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer A viewport into the framebuffer to copy from.
     * @param readAttachmentIndex The index of the attachment within the read framebuffer to copy from.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     */
    void blitColorAttachmentFromFramebufferViewport(int drawAttachmentIndex, int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer, int readAttachmentIndex, boolean linearFiltering);

    /**
     * Copies pixels from one framebuffer to another.
     * The copying operation will be start at the lower left corner of this framebuffer, and will preserve the resolution of the read framebuffer
     * @param drawAttachmentIndex The index of the color attachment of this framebuffer to copy into.
     * @param readFramebuffer The framebuffer to copy from.
     * @param readAttachmentIndex The index of the attachment within the read framebuffer to copy from.
     */
    default void blitColorAttachmentFromFramebuffer(int drawAttachmentIndex, Framebuffer<ContextType> readFramebuffer, int readAttachmentIndex)
    {
        blitColorAttachmentFromFramebuffer(drawAttachmentIndex, 0, 0, readFramebuffer, readAttachmentIndex);
    }

    /**
     * Copies pixels from one framebuffer to another.
     * The copying operation will be start at (x, y) within this framebuffer, and will preserve the resolution of the read framebuffer
     * @param drawAttachmentIndex The index of the color attachment of this framebuffer to copy into.
     * @param x The left edge of the rectangle to copy into within this framebuffer.
     * @param y The bottom edge of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer The framebuffer to copy from.
     * @param readAttachmentIndex The index of the attachment within the read framebuffer to copy from.
     */
    default void blitColorAttachmentFromFramebuffer(int drawAttachmentIndex, int x, int y, Framebuffer<ContextType> readFramebuffer, int readAttachmentIndex)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitColorAttachmentFromFramebufferViewport(drawAttachmentIndex, x, y, size.width, size.height,
            readFramebuffer.getViewport(0, 0, size.width, size.height), readAttachmentIndex, false);
    }

    /**
     * Copies pixels from one framebuffer to another.
     * The copying operation will span the entirety of both framebuffers, resizing it the framebuffer resolutions are not the same.
     * @param drawAttachmentIndex The index of the color attachment of this framebuffer to copy into.
     * @param readFramebuffer The framebuffer to copy from.
     * @param readAttachmentIndex The index of the attachment within the read framebuffer to copy from.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     */
    default void blitScaledColorAttachmentFromFramebuffer(
        int drawAttachmentIndex, Framebuffer<ContextType> readFramebuffer, int readAttachmentIndex, boolean linearFiltering)
    {
        FramebufferSize readSize = readFramebuffer.getSize();
        FramebufferSize drawSize = this.getSize();
        blitColorAttachmentFromFramebufferViewport(drawAttachmentIndex, 0, 0, drawSize.width, drawSize.height,
            readFramebuffer.getViewport(0, 0, readSize.width, readSize.height), readAttachmentIndex, linearFiltering);
    }

    /**
     * Copies pixels from a viewport within one framebuffer depth attachment to another framebuffer.
     * Due to OpenGL limitations, linear filtering will not be applied.
     * @param destX The left edge of the rectangle to copy into within this framebuffer.
     * @param destY The bottom edge of the rectangle to copy into within this framebuffer.
     * @param destWidth The width of the rectangle to copy into within this framebuffer.
     * @param destHeight The height of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer A viewport into the framebuffer to copy from.
     */
    void blitDepthAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer);

    /**
     * Copies pixels from one framebuffer depth attachment to another.
     * The copying operation will be start at the lower left corner of this framebuffer, and will preserve the resolution of the read framebuffer
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitDepthAttachmentFromFramebuffer(Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitDepthAttachmentFromFramebuffer(0, 0, readFramebuffer);
    }

    /**
     * Copies pixels from one framebuffer depth attachment to another.
     * The copying operation will be start at (x, y) within this framebuffer, and will preserve the resolution of the read framebuffer
     * @param x The left edge of the rectangle to copy into within this framebuffer.
     * @param y The bottom edge of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitDepthAttachmentFromFramebuffer(int x, int y, Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitDepthAttachmentFromFramebufferViewport(x, y, size.width, size.height,
            readFramebuffer.getViewport(0, 0, size.width, size.height));
    }

    /**
     * Copies pixels from one framebuffer depth attachment to another.
     * The copying operation will span the entirety of both framebuffers, resizing it the framebuffer resolutions are not the same.
     * Due to OpenGL limitations, linear filtering will not be applied.
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitScaledDepthAttachmentFromFramebuffer(Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize readSize = readFramebuffer.getSize();
        FramebufferSize drawSize = this.getSize();
        blitDepthAttachmentFromFramebufferViewport(0, 0, drawSize.width, drawSize.height,
            readFramebuffer.getViewport(0, 0, readSize.width, readSize.height));
    }

    /**
     * Copies pixels from a viewport within one framebuffer stencil attachment to another framebuffer.
     * Due to OpenGL limitations, linear filtering will not be applied.
     * @param destX The left edge of the rectangle to copy into within this framebuffer.
     * @param destY The bottom edge of the rectangle to copy into within this framebuffer.
     * @param destWidth The width of the rectangle to copy into within this framebuffer.
     * @param destHeight The height of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer A viewport into the framebuffer to copy from.
     */
    void blitStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer);

    /**
     * Copies pixels from one framebuffer stencil attachment to another.
     * The copying operation will be start at the lower left corner of this framebuffer, and will preserve the resolution of the read framebuffer
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitStencilAttachmentFromFramebuffer(Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitStencilAttachmentFromFramebuffer(0, 0, readFramebuffer);
    }

    /**
     * Copies pixels from one framebuffer stencil attachment to another.
     * The copying operation will be start at (x, y) within this framebuffer, and will preserve the resolution of the read framebuffer
     * @param x The left edge of the rectangle to copy into within this framebuffer.
     * @param y The bottom edge of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitStencilAttachmentFromFramebuffer(int x, int y, Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitStencilAttachmentFromFramebufferViewport(x, y, size.width, size.height,
            readFramebuffer.getViewport(0, 0, size.width, size.height));
    }

    /**
     * Copies pixels from one framebuffer stencil attachment to another.
     * The copying operation will span the entirety of both framebuffers, resizing it the framebuffer resolutions are not the same.
     * Due to OpenGL limitations, linear filtering will not be applied.
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitScaledStencilAttachmentFromFramebuffer(Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize readSize = readFramebuffer.getSize();
        FramebufferSize drawSize = this.getSize();
        blitStencilAttachmentFromFramebufferViewport(0, 0, drawSize.width, drawSize.height,
            readFramebuffer.getViewport(0, 0, readSize.width, readSize.height));
    }

    /**
     * Copies pixels from a viewport within one framebuffer's depth and stencil attachments to another framebuffer.
     * Due to OpenGL limitations, linear filtering will not be applied.
     * @param destX The left edge of the rectangle to copy into within this framebuffer.
     * @param destY The bottom edge of the rectangle to copy into within this framebuffer.
     * @param destWidth The width of the rectangle to copy into within this framebuffer.
     * @param destHeight The height of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer A viewport into the framebuffer to copy from.
     */
    void blitDepthStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer);

    /**
     * Copies pixels from one framebuffer's depth and stencil attachments to another.
     * The copying operation will be start at the lower left corner of this framebuffer, and will preserve the resolution of the read framebuffer
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitDepthStencilAttachmentFromFramebuffer(Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitDepthStencilAttachmentFromFramebuffer(0, 0, readFramebuffer);
    }

    /**
     * Copies pixels from one framebuffer's depth and stencil attachments to another.
     * The copying operation will be start at (x, y) within this framebuffer, and will preserve the resolution of the read framebuffer
     * @param x The left edge of the rectangle to copy into within this framebuffer.
     * @param y The bottom edge of the rectangle to copy into within this framebuffer.
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitDepthStencilAttachmentFromFramebuffer(int x, int y, Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize size = readFramebuffer.getSize();
        blitDepthStencilAttachmentFromFramebufferViewport(x, y, size.width, size.height,
            readFramebuffer.getViewport(0, 0, size.width, size.height));
    }

    /**
     * Copies pixels from one framebuffer's depth and stencil attachments to another.
     * The copying operation will span the entirety of both framebuffers, resizing it the framebuffer resolutions are not the same.
     * Due to OpenGL limitations, linear filtering will not be applied.
     * @param readFramebuffer The framebuffer to copy from.
     */
    default void blitScaledDepthStencilAttachmentFromFramebuffer(Framebuffer<ContextType> readFramebuffer)
    {
        FramebufferSize readSize = readFramebuffer.getSize();
        FramebufferSize drawSize = this.getSize();
        blitDepthStencilAttachmentFromFramebufferViewport(0, 0, drawSize.width, drawSize.height,
            readFramebuffer.getViewport(0, 0, readSize.width, readSize.height));
    }
}