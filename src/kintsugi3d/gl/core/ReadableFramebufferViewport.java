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

import kintsugi3d.gl.vecmath.IntVector2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class ReadableFramebufferViewport<ContextType extends Context<ContextType>>
    extends FramebufferViewport<ContextType>
    implements ReadableFramebuffer<ContextType>
{
    private static class ReadContents<ContextType extends Context<ContextType>> implements FramebufferReadContents<ContextType>
    {
        private final FramebufferViewport<ContextType> framebufferViewport;

        private final FramebufferReadContents<ContextType> fullFramebufferContents;

        ReadContents(FramebufferViewport<ContextType> framebufferViewport, FramebufferReadContents<ContextType> fullFramebufferContents)
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
        public void bindForRead(int attachmentIndex)
        {
            fullFramebufferContents.bindForRead(attachmentIndex);
        }

        @Override
        public void bindNonColorAttachmentForRead()
        {
            fullFramebufferContents.bindNonColorAttachmentForRead();
        }
    }

    private final ReadableFramebuffer<ContextType> readableFramebuffer;

    ReadableFramebufferViewport(
        ReadableFramebuffer<ContextType> fullFramebuffer, IntVector2 viewportOffset, FramebufferSize viewportSize)
    {
        super(fullFramebuffer, viewportOffset, viewportSize);
        this.readableFramebuffer = fullFramebuffer;
    }

    @Override
    public FramebufferReadContents<ContextType> getReadContents()
    {
        return new ReadContents<>(this, readableFramebuffer.getReadContents());
    }

    @Override
    public ColorTextureReader getTextureReaderForColorAttachment(int attachmentIndex)
    {
        return new ColorTextureReaderBase()
        {
            @Override
            public int getWidth()
            {
                return getSize().width;
            }

            @Override
            public int getHeight()
            {
                return getSize().height;
            }

            @Override
            public void readARGB(ByteBuffer destination, int x, int y, int width, int height)
            {
                readableFramebuffer.getTextureReaderForColorAttachment(attachmentIndex)
                    .readARGB(destination, x + getOffset().x, y + getOffset().y, width, height);
            }

            @Override
            public void readFloatingPointRGBA(FloatBuffer destination, int x, int y, int width, int height)
            {
                readableFramebuffer.getTextureReaderForColorAttachment(attachmentIndex)
                    .readFloatingPointRGBA(destination, x + getOffset().x, y + getOffset().y, width, height);
            }

            @Override
            public void readIntegerRGBA(IntBuffer destination, int x, int y, int width, int height)
            {
                readableFramebuffer.getTextureReaderForColorAttachment(attachmentIndex)
                    .readIntegerRGBA(destination, x + getOffset().x, y + getOffset().y, width, height);
            }
        };
    }

    @Override
    public DepthTextureReader getTextureReaderForDepthAttachment()
    {
        return new DepthTextureReaderBase()
        {
            @Override
            public int getWidth()
            {
                return getSize().width;
            }

            @Override
            public int getHeight()
            {
                return getSize().height;
            }

            @Override
            public void read(ShortBuffer destination, int x, int y, int width, int height)
            {
                readableFramebuffer.getTextureReaderForDepthAttachment()
                    .read(destination, x + getOffset().x, y + getOffset().y, width, height);
            }
        };
    }

    /**
     * Gets an object that encapsulates a viewport within this framebuffer that can be drawn to.
     * @param x The left edge of the viewport
     * @param y The bottom edge of the viewport
     * @param width The width of the viewport
     * @param height The height of the viewport
     * @return
     */
    @Override
    public ReadableFramebufferViewport<ContextType> getViewport(int x, int y, int width, int height)
    {
        return new ReadableFramebufferViewport<>(this, new IntVector2(x, y), new FramebufferSize(width, height));
    }
}
