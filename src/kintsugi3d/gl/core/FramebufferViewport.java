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

import kintsugi3d.gl.vecmath.IntVector2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class FramebufferViewport<ContextType extends Context<ContextType>> implements Framebuffer<ContextType>
{
    private static class DrawContents<ContextType extends Context<ContextType>> implements FramebufferDrawContents<ContextType>
    {
        private final FramebufferViewport<ContextType> framebufferViewport;

        private final FramebufferDrawContents<ContextType> fullFramebufferContents;

        public DrawContents(FramebufferViewport<ContextType> framebufferViewport, FramebufferDrawContents<ContextType> fullFramebufferContents)
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
        public void bindViewportForDraw(int x, int y, int width, int height)
        {
            IntVector2 offset = framebufferViewport.getOffset();
            fullFramebufferContents.bindViewportForDraw(x + offset.x, y + offset.y, width, height);
        }

        @Override
        public void bindSingleAttachmentForDraw(int attachmentIndex)
        {
            fullFramebufferContents.bindSingleAttachmentForDraw(attachmentIndex);
        }

        @Override
        public void bindNonColorAttachmentsForDraw()
        {
            fullFramebufferContents.bindNonColorAttachmentsForDraw();
        }
    }
    private static class ReadContents<ContextType extends Context<ContextType>> implements FramebufferReadContents<ContextType>
    {
        private final FramebufferViewport<ContextType> framebufferViewport;

        private final FramebufferReadContents<ContextType> fullFramebufferContents;

        public ReadContents(FramebufferViewport<ContextType> framebufferViewport, FramebufferReadContents<ContextType> fullFramebufferContents)
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
    public FramebufferReadContents<ContextType> getReadContents()
    {
        return new ReadContents<>(this, fullFramebuffer.getReadContents());
    }

    @Override
    public FramebufferDrawContents<ContextType> getDrawContents()
    {
        return new DrawContents<>(this, fullFramebuffer.getDrawContents());
    }

    @Override
    public FramebufferSize getSize()
    {
        return viewportSize;
    }

    public IntVector2 getOffset()
    {
        return viewportOffset;
    }

    @Override
    public int getColorAttachmentCount()
    {
        return fullFramebuffer.getColorAttachmentCount();
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
                fullFramebuffer.getTextureReaderForColorAttachment(attachmentIndex)
                    .readARGB(destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
            }

            @Override
            public void readFloatingPointRGBA(FloatBuffer destination, int x, int y, int width, int height)
            {
                fullFramebuffer.getTextureReaderForColorAttachment(attachmentIndex)
                    .readFloatingPointRGBA(destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
            }

            @Override
            public void readIntegerRGBA(IntBuffer destination, int x, int y, int width, int height)
            {
                fullFramebuffer.getTextureReaderForColorAttachment(attachmentIndex)
                    .readIntegerRGBA(destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
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
                fullFramebuffer.getTextureReaderForDepthAttachment()
                    .read(destination, x + viewportOffset.x, y + viewportOffset.y, width, height);
            }
        };
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

    @Override
    public FramebufferViewport<ContextType> getViewport(int x, int y, int width, int height)
    {
        // Make sure that a FramebufferViewport never has another FramebufferViewport as its "full framebuffer"
        return new FramebufferViewport<>(fullFramebuffer,
            new IntVector2(x + this.viewportOffset.x, y + this.viewportOffset.y), new FramebufferSize(width, height));
    }

    @Override
    public void blitColorAttachmentFromFramebufferViewport(int drawAttachmentIndex, int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer, int readAttachmentIndex, boolean linearFiltering)
    {
        fullFramebuffer.blitColorAttachmentFromFramebufferViewport(
            drawAttachmentIndex, destX + viewportOffset.x, destY + viewportOffset.y, destWidth, destHeight,
            readFramebuffer, readAttachmentIndex, linearFiltering);
    }

    @Override
    public void blitDepthAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer)
    {
        fullFramebuffer.blitDepthAttachmentFromFramebufferViewport(
            destX + viewportOffset.x, destY + viewportOffset.y, destWidth, destHeight, readFramebuffer);
    }

    @Override
    public void blitStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer)
    {
        fullFramebuffer.blitStencilAttachmentFromFramebufferViewport(
            destX + viewportOffset.x, destY + viewportOffset.y, destWidth, destHeight, readFramebuffer);
    }

    @Override
    public void blitDepthStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<ContextType> readFramebuffer)
    {
        fullFramebuffer.blitDepthStencilAttachmentFromFramebufferViewport(
            destX + viewportOffset.x, destY + viewportOffset.y, destWidth, destHeight, readFramebuffer);
    }
}
