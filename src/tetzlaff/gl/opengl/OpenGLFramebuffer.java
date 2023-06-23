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

package tetzlaff.gl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.*;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

abstract class OpenGLFramebuffer extends tetzlaff.gl.core.FramebufferBase<OpenGLContext> implements Framebuffer<OpenGLContext>
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

    abstract class ContentsBase implements tetzlaff.gl.core.FramebufferContents<OpenGLContext>
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

        @Override
        public void bindForDraw(int x, int y, int width, int height)
        {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
            OpenGLContext.errorCheck();

            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            {
                OpenGLContext.throwInvalidFramebufferOperationException();
            }
            OpenGLContext.errorCheck();

            glViewport(x, y, width, height);
            OpenGLContext.errorCheck();
        }

        @Override
        public void bindForDraw()
        {
            FramebufferSize size = OpenGLFramebuffer.this.getSize();
            this.bindForDraw(0, 0, size.width, size.height);
        }

        abstract void selectColorSourceForRead(int index);

        @Override
        public void bindForRead(int attachmentIndex)
        {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
            OpenGLContext.errorCheck();

            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            {
                OpenGLContext.throwInvalidFramebufferOperationException();
            }
            OpenGLContext.errorCheck();

            selectColorSourceForRead(attachmentIndex);
        }
    }

    @Override
    public abstract ContentsBase getContents();

    @Override
    public void readColorBufferARGB(int attachmentIndex, ByteBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height * 4)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContents().bindForRead(attachmentIndex);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        // use BGRA because due to byte order differences it ends up being ARGB
        glReadPixels(x, y, width, height, GL_BGRA, GL_UNSIGNED_BYTE, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height * 4)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContents().bindForRead(attachmentIndex);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_RGBA, GL_FLOAT, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height * 4)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContents().bindForRead(attachmentIndex);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_RGBA_INTEGER, GL_INT, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readDepthBuffer(ShortBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContents().bindForRead(0);

        glPixelStorei(GL_PACK_ALIGNMENT, 2);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, destination);
        OpenGLContext.errorCheck();
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
    }
}
