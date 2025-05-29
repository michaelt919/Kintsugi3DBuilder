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

package kintsugi3d.gl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.text.MessageFormat;

import org.lwjgl.*;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.IntVector2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

abstract class OpenGLFramebuffer implements Framebuffer<OpenGLContext>
{
    protected final OpenGLContext context;

    OpenGLFramebuffer(OpenGLContext context)
    {
        this.context = context;
    }

    @Override
    public OpenGLContext getContext()
    {
        return this.context;
    }

    abstract class ContentsBase implements FramebufferDrawContents<OpenGLContext>, FramebufferReadContents<OpenGLContext>
    {
        private final OpenGLContext context;

        protected ContentsBase()
        {
            this.context = OpenGLFramebuffer.this.context;
        }

        abstract int getId();

        @Override
        public OpenGLContext getContext()
        {
            return context;
        }

        @Override
        public FramebufferSize getSize()
        {
            return OpenGLFramebuffer.this.getSize();
        }

        private void bindForDrawCommon(int x, int y, int width, int height)
        {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
            OpenGLContext.errorCheck();

            if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            {
                OpenGLContext.throwInvalidFramebufferOperationException();
            }
            OpenGLContext.errorCheck();

            glViewport(x, y, width, height);
            OpenGLContext.errorCheck();
        }

        @Override
        public void bindViewportForDraw(int x, int y, int width, int height)
        {
            bindForDrawCommon(x, y, width, height);
            useAllColorDestinationsForDraw();
        }

        @Override
        public void bindForDraw()
        {
            FramebufferSize size = OpenGLFramebuffer.this.getSize();
            this.bindViewportForDraw(0, 0, size.width, size.height);
        }

        /**
         * Select just a single color attachment for draw - used primarily for blit operations
         * @param index
         */
        abstract void selectColorDestinationForDraw(int index);

        /**
         * Use all color attachments for draw - as would typically be the case when doing normal drawing.
         */
        abstract void useAllColorDestinationsForDraw();

        @Override
        public void bindSingleAttachmentForDraw(int attachmentIndex)
        {
            FramebufferSize size = OpenGLFramebuffer.this.getSize();
            this.bindForDrawCommon(0, 0, size.width, size.height);

            selectColorDestinationForDraw(attachmentIndex);
        }

        @Override
        public void bindNonColorAttachmentsForDraw()
        {
            FramebufferSize size = OpenGLFramebuffer.this.getSize();
            this.bindForDrawCommon(0, 0, size.width, size.height);
        }

        abstract void selectColorSourceForRead(int index);

        private void bindForReadCommon()
        {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
            OpenGLContext.errorCheck();

            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            {
                OpenGLContext.throwInvalidFramebufferOperationException();
            }
            OpenGLContext.errorCheck();
        }

        @Override
        public void bindForRead(int attachmentIndex)
        {
            bindForReadCommon();
            selectColorSourceForRead(attachmentIndex);
        }

        @Override
        public void bindNonColorAttachmentForRead()
        {
            bindForReadCommon();
        }
    }

    @Override
    public FramebufferReadContents<OpenGLContext> getReadContents()
    {
        return getContents();
    }

    @Override
    public FramebufferDrawContents<OpenGLContext> getDrawContents()
    {
        return getContents();
    }

    protected abstract ContentsBase getContents();

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
                if (destination.remaining() < width * height * 4)
                {
                    throw new IllegalArgumentException(
                        MessageFormat.format(
                            "The destination buffer (capacity: {0}, limit: {1}, position: {2}, remaining: {3}) " +
                                "is not big enough to hold the requested data (width: {4}, height: {5}).",
                            destination.capacity(), destination.limit(),  destination.position(), destination.remaining(),
                            width, height));
                }

                getReadContents().bindForRead(attachmentIndex);

                glPixelStorei(GL_PACK_ALIGNMENT, 4);
                OpenGLContext.errorCheck();

                // use BGRA because due to byte order differences it ends up being ARGB
                glReadPixels(x, y, width, height, GL_BGRA, GL_UNSIGNED_BYTE, destination);
                OpenGLContext.errorCheck();
            }

            @Override
            public void readFloatingPointRGBA(FloatBuffer destination, int x, int y, int width, int height)
            {
                if (destination.remaining() < width * height * 4)
                {
                    throw new IllegalArgumentException(
                        MessageFormat.format(
                            "The destination buffer (capacity: {0}, limit: {1}, position: {2}, remaining: {3}) " +
                                "is not big enough to hold the requested data (width: {4}, height: {5}).",
                            destination.capacity(), destination.limit(),  destination.position(), destination.remaining(),
                            width, height));
                }

                getReadContents().bindForRead(attachmentIndex);

                glPixelStorei(GL_PACK_ALIGNMENT, 4);
                OpenGLContext.errorCheck();

                glReadPixels(x, y, width, height, GL_RGBA, GL_FLOAT, destination);
                OpenGLContext.errorCheck();
            }

            @Override
            public void readIntegerRGBA(IntBuffer destination, int x, int y, int width, int height)
            {
                if (destination.remaining() < width * height * 4)
                {
                    throw new IllegalArgumentException(
                        MessageFormat.format(
                            "The destination buffer (capacity: {0}, limit: {1}, position: {2}, remaining: {3}) " +
                                "is not big enough to hold the requested data (width: {4}, height: {5}).",
                            destination.capacity(), destination.limit(),  destination.position(), destination.remaining(),
                            width, height));
                }

                getReadContents().bindForRead(attachmentIndex);

                glPixelStorei(GL_PACK_ALIGNMENT, 4);
                OpenGLContext.errorCheck();

                glReadPixels(x, y, width, height, GL_RGBA_INTEGER, GL_INT, destination);
                OpenGLContext.errorCheck();
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
                if (destination.remaining() < width * height)
                {
                    throw new IllegalArgumentException(
                        MessageFormat.format(
                            "The destination buffer (capacity: {0}, limit: {1}, position: {2}, remaining: {3}) " +
                                "is not big enough to hold the requested data (width: {4}, height: {5}).",
                            destination.capacity(), destination.limit(),  destination.position(), destination.remaining(),
                            width, height));
                }

                getReadContents().bindForRead(0);

                glPixelStorei(GL_PACK_ALIGNMENT, 2);
                OpenGLContext.errorCheck();

                glReadPixels(x, y, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, destination);
                OpenGLContext.errorCheck();
            }
        };
    }

    @Override
    public void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, getContents().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.flip();
        glClearBufferfv(GL_COLOR, attachmentIndex, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, getContents().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        IntBuffer buffer = BufferUtils.createIntBuffer(4);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.flip();
        glClearBufferiv(GL_COLOR, attachmentIndex, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearDepthBuffer(float depth)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, getContents().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(1);
        buffer.put(depth);
        buffer.flip();
        glClearBufferfv(GL_DEPTH, 0, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearStencilBuffer(int stencilIndex)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, getContents().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        buffer.put(stencilIndex);
        buffer.flip();
        glClearBufferiv(GL_STENCIL, 0, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a, int x, int y, int width, int height)
    {
        // Use scissor test to only clear the rectangle specified.
        glEnable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();

        glScissor(x, y, width, height);
        OpenGLContext.errorCheck();

        clearColorBuffer(attachmentIndex, r, g, b, a);

        glDisable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a, int x, int y, int width, int height)
    {
        // Use scissor test to only clear the rectangle specified.
        glEnable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();

        glScissor(x, y, width, height);
        OpenGLContext.errorCheck();

        clearIntegerColorBuffer(attachmentIndex, r, g, b, a);

        glDisable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearDepthBuffer(float depth, int x, int y, int width, int height)
    {
        // Use scissor test to only clear the rectangle specified.
        glEnable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();

        glScissor(x, y, width, height);
        OpenGLContext.errorCheck();

        clearDepthBuffer(depth);

        glDisable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearStencilBuffer(int stencilIndex, int x, int y, int width, int height)
    {
        // Use scissor test to only clear the rectangle specified.
        glEnable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();

        glScissor(x, y, width, height);
        OpenGLContext.errorCheck();

        clearStencilBuffer(stencilIndex);

        glDisable(GL_SCISSOR_TEST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void blitColorAttachmentFromFramebufferViewport(
        int drawAttachmentIndex, int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<OpenGLContext> readFramebuffer, int readAttachmentIndex, boolean linearFiltering)
    {
        this.getDrawContents().bindSingleAttachmentForDraw(drawAttachmentIndex);
        readFramebuffer.getReadContents().bindForRead(readAttachmentIndex);

        IntVector2 srcOffset = readFramebuffer.getOffset();
        FramebufferSize srcSize = readFramebuffer.getSize();
        glBlitFramebuffer(srcOffset.x, srcOffset.y, srcOffset.x + srcSize.width, srcOffset.y + srcSize.height,
            destX, destY, destX + destWidth, destY + destHeight,
            GL_COLOR_BUFFER_BIT, linearFiltering ? GL_LINEAR : GL_NEAREST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void blitDepthAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<OpenGLContext> readFramebuffer)
    {
        this.getDrawContents().bindNonColorAttachmentsForDraw();
        readFramebuffer.getReadContents().bindNonColorAttachmentForRead();

        IntVector2 srcOffset = readFramebuffer.getOffset();
        FramebufferSize srcSize = readFramebuffer.getSize();
        glBlitFramebuffer(srcOffset.x, srcOffset.y, srcOffset.x + srcSize.width, srcOffset.y + srcSize.height,
            destX, destY, destX + destWidth, destY + destHeight,
            GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void blitStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<OpenGLContext> readFramebuffer)
    {
        this.getDrawContents().bindNonColorAttachmentsForDraw();
        readFramebuffer.getReadContents().bindNonColorAttachmentForRead();

        IntVector2 srcOffset = readFramebuffer.getOffset();
        FramebufferSize srcSize = readFramebuffer.getSize();
        glBlitFramebuffer(srcOffset.x, srcOffset.y, srcOffset.x + srcSize.width, srcOffset.y + srcSize.height,
            destX, destY, destX + destWidth, destY + destHeight,
            GL_STENCIL_BUFFER_BIT, GL_NEAREST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void blitDepthStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
        FramebufferViewport<OpenGLContext> readFramebuffer)
    {
        this.getDrawContents().bindNonColorAttachmentsForDraw();
        readFramebuffer.getReadContents().bindNonColorAttachmentForRead();

        IntVector2 srcOffset = readFramebuffer.getOffset();
        FramebufferSize srcSize = readFramebuffer.getSize();
        glBlitFramebuffer(srcOffset.x, srcOffset.y, srcOffset.x + srcSize.width, srcOffset.y + srcSize.height,
            destX, destY, destX + destWidth, destY + destHeight,
            GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT, GL_NEAREST);
        OpenGLContext.errorCheck();
    }
}
