/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

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
    Object getContentsForRead();
    Object getContentsForWrite();

    FramebufferSize getSize();

    int getColorAttachmentCount();

    void readColorBufferARGB(int attachmentIndex, ByteBuffer destination, int x, int y, int width, int height);
    void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination, int x, int y, int width, int height);
    void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination, int x, int y, int width, int height);
    void readDepthBuffer(ShortBuffer destination, int x, int y, int width, int height);

    void readColorBufferARGB(int attachmentIndex, ByteBuffer destination);
    void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination);
    void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination);
    void readDepthBuffer(ShortBuffer destination);

    int[] readColorBufferARGB(int attachmentIndex);
    int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height);

    float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);
    float[] readFloatingPointColorBufferRGBA(int attachmentIndex);

    int[] readIntegerColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);
    int[] readIntegerColorBufferRGBA(int attachmentIndex);

    short[] readDepthBuffer(int x, int y, int width, int height);
    short[] readDepthBuffer();

    void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException;
    void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException;

    void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a);
    void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a);
    void clearDepthBuffer(float depth);
    void clearDepthBuffer();
    void clearStencilBuffer(int stencilIndex);
}