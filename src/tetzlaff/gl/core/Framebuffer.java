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
public interface Framebuffer<ContextType extends Context<ContextType>> extends Contextual<ContextType>
{
    /**
     * Gets a representation of the contents of this framebuffer .
     * @return A handle that can be used to perform operations that retrieve or modify the contents of this framebuffer.
     */
    FramebufferContents<ContextType> getContents();

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
     * Reads the pixels currently in one of the framebuffer's attachments into an existing buffer in memory.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readColorBufferARGB(int attachmentIndex, ByteBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as 32-bit integers.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in the framebuffer's depth attachment.
     * Only a rectangular subset of the pixels will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readDepthBuffer(ShortBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in one of the framebuffer's attachments.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    default void readColorBufferARGB(int attachmentIndex, ByteBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readColorBufferARGB(attachmentIndex, destination, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    default void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readFloatingPointColorBufferRGBA(attachmentIndex, destination, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as 32-bit integers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    default void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readIntegerColorBufferRGBA(attachmentIndex, destination, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in the framebuffer's depth attachment.
     * The entire framebuffer will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    default void readDepthBuffer(ShortBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readDepthBuffer(destination, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @return An array containing the pixels.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    default int[] readColorBufferARGB(int attachmentIndex)
    {
        FramebufferSize size = this.getSize();
        return this.readColorBufferARGB(attachmentIndex, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the pixels.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the pixels as floating point numbers.
     */
    float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @return An array containing the pixels as floating point numbers.
     */
    default float[] readFloatingPointColorBufferRGBA(int attachmentIndex)
    {
        FramebufferSize size = this.getSize();
        return this.readFloatingPointColorBufferRGBA(attachmentIndex, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as 32-bit integers.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the pixels as floating point numbers.
     */
    int[] readIntegerColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);

    /**
     * Reads the pixels currently in one of the framebuffer's attachments as 32-bit integers.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @return An array containing the pixels as floating point numbers.
     */
    default int[] readIntegerColorBufferRGBA(int attachmentIndex)
    {
        FramebufferSize size = this.getSize();
        return this.readIntegerColorBufferRGBA(attachmentIndex, 0, 0, size.width, size.height);
    }

    /**
     * Reads the pixels currently in the framebuffer's depth attachment.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the depth values.
     */
    short[] readDepthBuffer(int x, int y, int width, int height);

    /**
     * Reads the pixels currently in the framebuffer's depth attachment.
     * The entire framebuffer will be read.
     * @return An array containing the depth values.
     */
    default short[] readDepthBuffer()
    {
        FramebufferSize size = this.getSize();
        return this.readDepthBuffer(0, 0, size.width, size.height);
    }

    /**
     * Saves the the pixels currently in one of the framebuffer's attachments to a file.
     * The entire framebuffer will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException;

    /**
     * Saves the the pixels currently in one of the framebuffer's attachments to a file.
     * Only a rectangular subset of the pixels will be read.
     * @param attachmentIndex The index of the framebuffer attachment to be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException;

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

    default Framebuffer<ContextType> getViewport(int x, int y, int width, int height)
    {
        return new FramebufferViewport<>(this, new IntVector2(x, y), new FramebufferSize(width, height));
    }
}