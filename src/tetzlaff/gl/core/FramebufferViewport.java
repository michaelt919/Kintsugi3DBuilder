/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.core;

import tetzlaff.gl.vecmath.IntVector2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class FramebufferViewport<ContextType extends Context<ContextType>> extends FramebufferBase<ContextType>
{
    private static class Contents<ContextType extends Context<ContextType>> implements FramebufferContents<ContextType>
    {
        private final FramebufferViewport<ContextType> framebufferViewport;

        private final FramebufferContents<ContextType> fullFramebufferContents;

        public Contents(FramebufferViewport<ContextType> framebufferViewport, FramebufferContents<ContextType> fullFramebufferContents)
        {
            this.framebufferViewport = framebufferViewport;
            this.fullFramebufferContents = fullFramebufferContents;
        }

        @Override
        public ContextType getContext()
        {
            return framebufferViewport.getContext();
        }

        @Override
        public FramebufferSize getSize()
        {
            return framebufferViewport.getSize();
        }

        @Override
        public void bindForDraw(int x, int y, int width, int height)
        {
            IntVector2 offset = framebufferViewport.getOffset();
            fullFramebufferContents.bindForDraw(x + offset.x, y + offset.y, width, height);
        }

        @Override
        public void bindForRead(int attachmentIndex)
        {
            fullFramebufferContents.bindForRead(attachmentIndex);
        }
    }

    private final Framebuffer<ContextType> fullFramebuffer;

    private final IntVector2 viewportOffset;
    private final FramebufferSize viewportSize;

    FramebufferViewport(Framebuffer<ContextType> fullFramebuffer, IntVector2 viewportOffset, FramebufferSize viewportSize)
    {
        this.fullFramebuffer = fullFramebuffer;
        this.viewportOffset = viewportOffset;
        this.viewportSize = viewportSize;
    }

    @Override
    public ContextType getContext()
    {
        return fullFramebuffer.getContext();
    }

    @Override
    public FramebufferContents<ContextType> getContents()
    {
        return new Contents<>(this, fullFramebuffer.getContents());
    }

    @Override
    public FramebufferSize getSize()
    {
        return viewportSize;
    }

    private IntVector2 getOffset()
    {
        return viewportOffset;
    }

    @Override
    public int getColorAttachmentCount()
    {
        return fullFramebuffer.getColorAttachmentCount();
    }

    @Override
    public void readColorBufferARGB(int attachmentIndex, ByteBuffer destination, int x, int y, int width, int height)
    {
        fullFramebuffer.readColorBufferARGB(attachmentIndex, destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination, int x, int y, int width, int height)
    {
        fullFramebuffer.readFloatingPointColorBufferRGBA(attachmentIndex, destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination, int x, int y, int width, int height)
    {
        fullFramebuffer.readIntegerColorBufferRGBA(attachmentIndex, destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void readDepthBuffer(ShortBuffer destination, int x, int y, int width, int height)
    {
        fullFramebuffer.readDepthBuffer(destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a, int x, int y, int width, int height)
    {
        fullFramebuffer.clearColorBuffer(attachmentIndex, r, g, b, a, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a, int x, int y, int width, int height)
    {
        fullFramebuffer.clearIntegerColorBuffer(attachmentIndex, r, g, b, a, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void clearDepthBuffer(float depth, int x, int y, int width, int height)
    {
        fullFramebuffer.clearDepthBuffer(depth, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }

    @Override
    public void clearStencilBuffer(int stencilIndex, int x, int y, int width, int height)
    {
        fullFramebuffer.clearStencilBuffer(stencilIndex, x + viewportOffset.x, y + viewportOffset.y, width, height);
    }
}
